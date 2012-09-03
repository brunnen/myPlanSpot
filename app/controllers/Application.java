package controllers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import models.User;
import play.Logger;
import play.libs.OAuth2;
import play.libs.WS;
import play.mvc.Before;
import play.mvc.Controller;
import play.data.validation.*;

import java.util.List;
import java.util.ArrayList;

import com.google.gson.JsonObject;
import java.util.Map;

public class Application extends Controller {

    public static OAuth2 FACEBOOK = new OAuth2(
            "https://graph.facebook.com/oauth/authorize",
            "https://graph.facebook.com/oauth/access_token",
            play.Play.configuration.getProperty("myProperties.fb.app.client_id"),
            play.Play.configuration.getProperty("myProperties.fb.app.secret")
    );

    public static void index() {
        User u = connected();
        JsonObject me = null;
        JsonObject myEvents = null;

        if (u != null && u.access_token != null) {
            me = WS.url("https://graph.facebook.com/me?access_token=%s", WS.encode(u.access_token)).get().getJson().getAsJsonObject();
            myEvents = WS.url("https://graph.facebook.com/me/events?access_token=%s&fields=%s", WS.encode(u.access_token), "feed,name").get().getJson().getAsJsonObject();
            
            System.out.println(myEvents);
        }
        render(me, myEvents);
    }

    public static void auth() {
        if (OAuth2.isCodeResponse()) {
            User u = connected();
            OAuth2.Response response = FACEBOOK.retrieveAccessToken(authURL());
            u.access_token = response.accessToken;
            u.save();
            index();
        }
        FACEBOOK.retrieveVerificationCode(authURL());
    }

    @Before
    static void setuser() {
        User user = null;
        if (session.contains("uid")) {
            Logger.info("existing user: " + session.get("uid"));
            user = User.get(Long.parseLong(session.get("uid")));
        }
        if (user == null) {
            user = User.createNew();
            session.put("uid", user.uid);
        }
        renderArgs.put("user", user);
    }

    static String authURL() {
        return play.mvc.Router.getFullUrl("Application.auth");
    }

    static User connected() {
        return (User)renderArgs.get("user");
    }

    public static void postEvent(@Required String name, @Required String start_time){
        if (validation.hasErrors()) {
            params.flash(); // add http parameters to the flash scope
            validation.keep(); // keep the errors for the next request
        } else {
            User u = connected();
            JsonObject response = null;
            if (u != null && u.access_token != null) {
                response = WS.url("https://graph.facebook.com/me/events?access_token=%s&name=%s&start_time=%s", WS.encode(u.access_token), name, start_time).post().getJson().getAsJsonObject();
                System.out.println(response);
            }
        }
        index();
    }
    
    public static void postPost(@Required String ID, @Required String post){
        if (validation.hasErrors()) {
            params.flash(); // add http parameters to the flash scope
            validation.keep(); // keep the errors for the next request
        } else {
            User u = connected();
            JsonObject response = null;
            if (u != null && u.access_token != null) {
                response = WS.url("https://graph.facebook.com/%s/feed?access_token=%s&message=%s", ID, WS.encode(u.access_token), post).post().getJson().getAsJsonObject();
                System.out.println(response);
            }
        }
        index();
    }
}
