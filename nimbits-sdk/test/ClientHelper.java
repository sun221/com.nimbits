/*
 * Copyright (c) 2011. Tonic Solutions LLC. All Rights reserved.
 *
 * This source code is distributed under GPL v3 without any warranty.
 */

import com.nimbits.client.NimbitsClient;
import com.nimbits.client.NimbitsClientFactory;
import com.nimbits.client.model.Const;
import com.nimbits.client.model.common.CommonFactoryLocator;
import com.nimbits.client.model.email.EmailAddress;
import com.nimbits.client.model.entity.EntityName;
import com.nimbits.client.model.point.Point;
import com.nimbits.user.NimbitsUser;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

/**
 * Created by bsautner
 * User: benjamin
 * Date: 3/30/11
 * Time: 10:42 AM
 */
class ClientHelper {
    // private final static String hostURL = "http://nimbits1.appspot.com";

    //private final static String hostURL = "http://127.0.0.1:8888";
    // private final static String hostURL = "http://nimbits-qa.appspot.com";
    //   private final static String key = "cd5ecf67-f22b-4180-807e-b700c8ec98bc";


    private static NimbitsClient instance = null;
    private static NimbitsClient instance2 = null;
    // private final static String hostURL2 = "http://nimbits1.appspot.com";
    //  private final static String hostURLSpecific = "http://147.nimbits1.appspot.com";
    //  private static final String accountPath = "/mnt/raid/nimbits/test";
    private final static String email = "test@example.com";

    private final static String password = "81d6cbd4-c8a9-466e-b230-6e237dca599a";

    //  private final static String appscale =  "http://127.0.0.1:8888";
    public final static String url = "http://192.168.1.2:8081";

    public static NimbitsClient client() {
        if (instance == null) {
            //  String email = loadFile(accountPath + "/a1", 0);
            //  String password = loadFile(accountPath + "/a1", 1);
            EmailAddress em = CommonFactoryLocator.getInstance().createEmailAddress(email);
            NimbitsUser g = new NimbitsUser(em, password);

            try {
                instance = NimbitsClientFactory.getInstance(g, url);
            } catch (Exception e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        return instance;

    }


    public static Point createSeedPoint() {
        EntityName categoryName = CommonFactoryLocator.getInstance().createName(Const.CONST_HIDDEN_CATEGORY);
        EntityName pointName = CommonFactoryLocator.getInstance().createName(UUID.randomUUID().toString());
        Point point = client().addPoint(categoryName, pointName);
        Random r = new Random();


        for (int i = 0; i < 10; i++) {

            client().recordValue(pointName, r.nextDouble(), new Date());

        }

        return point;
    }


//    public static NimbitsClient meOnProd() {
//        if (instance == null) {
//            String email = loadFile(accountPath + "/a3", 0);
//            String password = loadFile(accountPath + "/a3", 1);
//            EmailAddress em = CommonFactoryLocator.getInstance().createEmailAddress(email);
//            NimbitsUser g = new NimbitsUser(em, password);
//
//            try {
//                instance = NimbitsClientFactory.getInstance(g, hostURL2);
//            } catch (Exception e) {
//                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//            }
//        }
//        return instance;
//
//    }


//    public static NimbitsClient clientSupportOnProd() {
//        if (instance2 == null) {
//            String email = loadFile(accountPath + "/a2", 0);
//            String password = loadFile(accountPath + "/a2", 1);
//            EmailAddress em = (EmailAddress) CommonFactoryLocator.getInstance().createEmailAddress(email);
//            GoogleUser g = new GoogleUser(em, password);
//            System.out.println(email);
//            try {
//                instance2 = NimbitsClientFactory.getInstance(g, hostURL);
//            } catch (Exception e) {
//                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//            }
//        }
//        return instance2;
//
//    }
//
//    public static NimbitsClient specificVersion() {
//        if (instance2 == null) {
//            String email = loadFile(accountPath + "/a2", 0);
//            String password = loadFile(accountPath + "/a2", 1);
//            EmailAddress em = CommonFactoryLocator.getInstance().createEmailAddress(email);
//            GoogleUser g = new GoogleUser(em, password);
//            System.out.println(email);
//            try {
//                instance2 = NimbitsClientFactory.getInstance(g, hostURLSpecific);
//            } catch (Exception e) {
//                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//            }
//        }
//        return instance2;
//
//    }
//
//    public static NimbitsClient authOnQA() {
//        if (instance2 == null) {
//            String email = loadFile(accountPath + "/a2", 0);
//            String password = loadFile(accountPath + "/a2", 1);
//            EmailAddress em = CommonFactoryLocator.getInstance().createEmailAddress(email);
//            GoogleUser g = new GoogleUser(em, password);
//            System.out.println(email);
//            try {
//                instance2 = NimbitsClientFactory.getInstance(g, hostURL);
//            } catch (Exception e) {
//                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//            }
//        }
//        return instance2;
//
//    }

    private static String loadFile(String fileName, int i) {

        java.lang.StringBuilder sb = new StringBuilder();
        BufferedReader in = null;
        String str;
        try {
            in = new BufferedReader(new FileReader(fileName));
            while ((str = in.readLine()) != null) {
                sb.append(str);
            }
            in.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


        return sb.toString().split(",")[i];  //To change body of implemented methods use File | Settings | File Templates.
    }

}
