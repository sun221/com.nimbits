/*
 * Copyright (c) 2010 Tonic Solutions LLC.
 *
 * http://www.nimbits.com
 *
 *
 * Licensed under the GNU GENERAL PUBLIC LICENSE, Version 3.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/gpl.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the license is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, eitherexpress or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.nimbits.server.dao.category;

import com.nimbits.PMF;
import com.nimbits.client.enums.EntityType;
import com.nimbits.client.enums.ProtectionLevel;
import com.nimbits.client.exception.NimbitsException;
import com.nimbits.client.model.Const;
import com.nimbits.client.model.category.Category;
import com.nimbits.client.model.category.CategoryModelFactory;

import com.nimbits.client.model.common.CommonFactoryLocator;
import com.nimbits.client.model.diagram.Diagram;
import com.nimbits.client.model.entity.EntityName;
import com.nimbits.client.model.point.Point;
import com.nimbits.client.model.point.PointModelFactory;
import com.nimbits.client.model.subscription.Subscription;
import com.nimbits.client.model.subscription.SubscriptionFactory;
import com.nimbits.client.model.user.User;
import com.nimbits.server.diagram.DiagramModelFactory;
import com.nimbits.server.orm.DataPoint;
import com.nimbits.server.orm.DiagramEntity;
import com.nimbits.server.orm.PointCatagory;
import com.nimbits.server.orm.SubscriptionEntity;
import com.nimbits.server.orm.entity.Entity;
import com.nimbits.server.point.PointServiceFactory;
import com.nimbits.server.pointcategory.CategoryTransactions;
import com.nimbits.server.task.TaskFactoryLocator;
import com.nimbits.shared.Utils;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;
import java.util.*;
import java.util.logging.Logger;

public class CategoryDAOImpl implements CategoryTransactions {

    private static final Logger log = Logger.getLogger(CategoryDAOImpl.class.getName());
    private final User user;

    public CategoryDAOImpl(User u) {
        user = u;
    }

    @Override
    public void purgeMemCache()  {

    }

    /* (non-Javadoc)
    * @see com.nimbits.server.pointcategory.CategoryDAO#createHiddenCategory(com.nimbits.client.model.user.NimbitsUser)
    */
    @Override
    public Category createHiddenCategory() {


        final PersistenceManager pm = PMF.get().getPersistenceManager();

        Category retObj;
        try {

            final EntityName categoryName = CommonFactoryLocator.getInstance().createName(Const.CONST_HIDDEN_CATEGORY);
            final Category c = new PointCatagory();
            c.setName(categoryName);
            c.setProtectionLevel(ProtectionLevel.onlyMe);
            c.setUUID(UUID.randomUUID().toString());
            c.setUserFK(user.getId());
            pm.makePersistent(c);

            retObj = CategoryModelFactory.createCategoryModel(c);
        } finally {
            pm.close();
        }


        return retObj;

    }

    /* (non-Javadoc)
      * @see com.nimbits.server.pointcategory.CategoryDAO#getCategories(com.nimbits.client.model.user.NimbitsUser, boolean)
      */
    @Override
    @SuppressWarnings(Const.WARNING_UNCHECKED)
    public List<Category> getCategories(final boolean includePoints,
                                        final boolean includeDiagrams,
                                        final boolean includeSubscriptions) {

        final PersistenceManager pm = PMF.get().getPersistenceManager();
        final LinkedList<Category> retObj = new LinkedList<Category>();
        final Query q = pm.newQuery(PointCatagory.class, "userFK == u");


        long userFK = user.getId();

        q.setOrdering("name ascending");
        q.declareParameters("Long u");

        try {
            List<Category> result = (List<Category>) q.execute(userFK);
            final Map<Long, List<Point>> points = includePoints ? getPointsByCategoryList(result) : null;
            final Map<Long, List<Diagram>> diagrams = includeDiagrams ? getDiagramsByCategoryList(result) : null;
            final Map<Long, List<Subscription>> subscriptions = includeSubscriptions ? getSubscriptionsByCategoryList(result) : null;
            for (final Category c : result) {


                if (points != null && includePoints) {
                    c.setPoints(points.get(c.getId()));
                }
                if (diagrams != null && includeDiagrams) {
                    c.setDiagrams(diagrams.get(c.getId()));
                }
                if (subscriptions != null && includeSubscriptions) {

                    List<Point> p = c.getPoints();
                    List<Subscription> subscriptionList = subscriptions.get(c.getId());
                    if (subscriptionList != null) {
                        for (Subscription subscription : subscriptionList) {
                            try {
                                Point sp = PointServiceFactory.getInstance().getPointByUUID(subscription.getSubscribedPointUUID());
                                if (sp != null && (sp.getUserFK() == user.getId() || sp.isPublic())) {
                                    sp.setReadOnly(true);
                                    sp.setEntityType(EntityType.subscription);
                                    p.add(sp);
                                }
                                c.setPoints(p);
                            } catch (NimbitsException e) {
                                log.severe(e.getMessage());
                            }
                        }
                    }

                }

            }


            for (final Category jdoCategory : result) {
                final Category r = CategoryModelFactory.createCategoryModel(jdoCategory);
                retObj.add(r);
            }

        } finally {
            pm.close();
        }
        return retObj;


    }


    private Map<Long, List<Point>> getPointsByCategoryList(final List<Category> categories) {
        final PersistenceManager pm = PMF.get().getPersistenceManager();


        Map<Long, List<Point>> retObj = null;
        final List<Point> points;
        List<Long> ids = new ArrayList<Long>();
        for (final Category c : categories) {
            ids.add(c.getId());
        }
        try {
            final Query q = pm.newQuery(DataPoint.class, ":p.contains(catID)");
            if (ids.size() > 0) {
                points = (List<Point>) q.execute(ids);


                List<Point> models = PointModelFactory.createPointModels(points);
                retObj = new HashMap<Long, List<Point>>();
                for (Point p : models) {
                    if (!retObj.containsKey(p.getCatID())) {
                        retObj.put(p.getCatID(), new ArrayList<Point>());
                    }
                    retObj.get(p.getCatID()).add(p);
                }
            }
        } finally {
            pm.close();
        }

        return retObj;
    }

    private Map<Long, List<Diagram>> getDiagramsByCategoryList(final List<Category> categories) {
        final PersistenceManager pm = PMF.get().getPersistenceManager();

        Map<Long, List<Diagram>> retObj = null;

        List<Long> ids = new ArrayList<Long>();
        for (final Category c : categories) {
            ids.add(c.getId());
        }
        try {
            final Query q = pm.newQuery(DiagramEntity.class, ":p.contains(categoryFk)");
            final List<Diagram> diagrams = (List<Diagram>) q.execute(ids);

            List<Diagram> models = DiagramModelFactory.createDiagramModels(diagrams);

            retObj = new HashMap<Long, List<Diagram>>();
            for (Diagram p : models) {
                if (!retObj.containsKey(p.getCategoryFk())) {
                    retObj.put(p.getCategoryFk(), new ArrayList<Diagram>());
                }
                retObj.get(p.getCategoryFk()).add(p);
            }
        } finally {
            pm.close();
        }

        return retObj;
    }
    private Map<Long, List<Subscription>> getSubscriptionsByCategoryList(final List<Category> categories) {
        final PersistenceManager pm = PMF.get().getPersistenceManager();

        Map<Long, List<Subscription>> retObj = null;

        List<Long> ids = new ArrayList<Long>();
        for (final Category c : categories) {
            ids.add(c.getId());
        }
        try {
            final Query q = pm.newQuery(SubscriptionEntity.class, ":p.contains(categoryId)");
            final List<Subscription> results = (List<Subscription>) q.execute(ids);

            List<Subscription> models =SubscriptionFactory.createSubscriptions(results);

            retObj = new HashMap<Long, List<Subscription>>();
            for (Subscription p : models) {
                if (!retObj.containsKey(p.getCategoryId())) {
                    retObj.put(p.getCategoryId(), new ArrayList<Subscription>());
                }
                retObj.get(p.getCategoryId()).add(p);
            }
        } finally {
            pm.close();
        }

        return retObj;
    }
    /* (non-Javadoc)
    * @see com.nimbits.server.pointcategory.CategoryDAO#getCategory(java.lang.String, long)
    */
    @Override
    public Category getCategory(final EntityName categoryName) {
        Category retObj = null;
        final PersistenceManager pm = PMF.get().getPersistenceManager();
        final Query q1 = pm.newQuery(PointCatagory.class, "name==u && userFK==l");
        q1.declareParameters("String u, Long l");
        q1.setRange(0, 1);

        try {
            @SuppressWarnings(Const.WARNING_UNCHECKED)
            final List<PointCatagory> c = (List<PointCatagory>) q1.execute(categoryName.getValue(),
                    user.getId());
            if (c.size() > 0) {
                retObj = CategoryModelFactory.createCategoryModel(c.get(0));
            }
        } finally {
            pm.close();
        }


        return retObj;
    }


    @Override
    public Category getCategory(final long id) {
        Category retObj = null;
        final PersistenceManager pm = PMF.get().getPersistenceManager();
        final Query q1 = pm.newQuery(PointCatagory.class, "id==i");
        q1.declareParameters("Long i");
        q1.setRange(0, 1);

        try {
            @SuppressWarnings(Const.WARNING_UNCHECKED)
            final List<PointCatagory> c = (List<PointCatagory>) q1.execute(id);
            if (c.size() > 0) {
                retObj = CategoryModelFactory.createCategoryModel(c.get(0));
            }
        } finally {
            pm.close();
        }


        return retObj;
    }

    /* (non-Javadoc)
      * @see com.nimbits.server.pointcategory.CategoryDAO#categoryExists(com.nimbits.client.model.user.NimbitsUser, java.lang.String)
      */
    @Override
    public boolean categoryExists(final EntityName EntityName) throws NimbitsException {
        throw new NimbitsException("Not Implemented");
    }


    @Override
    public Category addCategory(final EntityName name) {
        Category retObj;

        final PersistenceManager pm = PMF.get().getPersistenceManager();
        PointCatagory c;

        try {
            long userFK = user.getId();
            c = new PointCatagory(name);
            c.setProtectionLevel(ProtectionLevel.everyone);
            c.setUserFK(userFK);
            c.setUUID(UUID.randomUUID().toString());
            pm.makePersistent(c);

            Entity entity = new Entity(name, "",
                    EntityType.category,
                    ProtectionLevel.everyone,
                    UUID.randomUUID(),
                  null,
                    UUID.fromString(c.getUUID()),
                    UUID.fromString(user.getUuid()));

            pm.makePersistent(entity);

            retObj = CategoryModelFactory.createCategoryModel(c);
        } finally {
            pm.close();
        }
        return retObj;
    }


    /* (non-Javadoc)
      * @see com.nimbits.server.pointcategory.CategoryDAO#deleteCategory(com.nimbits.client.model.PointCatagory)
      */
    @Override
    public void deleteCategory(final Category c) {
        List<PointCatagory> cats;

        if (c == null) {
            return;

        }
        Transaction tx;

        PersistenceManager pm = PMF.get().getPersistenceManager();
        long catID = 0;

        // List<String> symbols = new ArrayList<String>();
        try {
            tx = pm.currentTransaction();
            tx.begin();

            Query q = pm.newQuery(PointCatagory.class, "id==k");

            q.declareParameters("String k");
            q.setRange(0, 1);
            cats = (List<PointCatagory>) q.execute(c.getId());
            if (cats.size() > 0) {
                catID = cats.get(0).getId();
                pm.deletePersistent(cats.get(0));

            }


            tx.commit();

            //delete points
            if (catID > 0) {
                List<DataPoint> points;
                //	ArrayList<DataPoint> retObj = new ArrayList<DataPoint>();
                try {
                    Query q4 = pm.newQuery(DataPoint.class, "catID == c");
                    q4.declareParameters("Long c");
                    points = (List<DataPoint>) q4.execute(catID);
                    for (Point dp : points) {
                        //PointCacheManager.remove(dp);
                        TaskFactoryLocator.getInstance().startDeleteDataTask(dp.getId(), false, 0, dp.getName());

                    }
                    pm.deletePersistentAll(points);
                } catch (Exception e) {
                    log.severe(e.getMessage());
                }
            }

            //delete subscriptions
            if (catID > 0) {
                List<Subscription> results;
                //	ArrayList<DataPoint> retObj = new ArrayList<DataPoint>();
                try {
                    Query q4 = pm.newQuery(SubscriptionEntity.class, "categoryId == c");
                    q4.declareParameters("Long c");
                    results = (List<Subscription>) q4.execute(catID);
                    pm.deletePersistentAll(results);
                } catch (Exception e) {
                    log.severe(e.getMessage());
                }
            }
        } catch (Exception e) {
            log.severe(e.getMessage());
        } finally {
            pm.close();
        }


    }

    @Override
    public Category updateCategory(final Category update) {
        Transaction tx;

        PersistenceManager pm = PMF.get().getPersistenceManager();

        try {
            tx = pm.currentTransaction();
            tx.begin();

            PointCatagory original = pm.getObjectById(PointCatagory.class, update.getId());
            original.setDescription(update.getDescription());
            original.setName(update.getName());
            original.setProtectionLevel(update.getProtectionLevel());
            if (Utils.isEmptyString(original.getUUID())) {
                original.setUUID(UUID.randomUUID().toString());
            } else {
                original.setUUID(update.getUUID());
            }


            tx.commit();
            return CategoryModelFactory.createCategoryModel(original);
        } catch (Exception ex) {
            return null;
        } finally {
            pm.close();
        }

    }

    @Override
    public Category getCategoryByUUID(final String uuidParam) {
        Category retObj = null;
        final PersistenceManager pm = PMF.get().getPersistenceManager();
        final Query q1 = pm.newQuery(PointCatagory.class, "uuid==i");
        q1.declareParameters("String i");
        q1.setRange(0, 1);

        try {
            @SuppressWarnings(Const.WARNING_UNCHECKED)
            final List<PointCatagory> c = (List<PointCatagory>) q1.execute(uuidParam);
            if (c.size() > 0) {
                retObj = CategoryModelFactory.createCategoryModel(c.get(0));
            }
        } finally {
            pm.close();
        }


        return retObj;
    }


}
