package com.alessandro.falldetector.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.google.gson.Gson;

public class SharedPreference {

    public static final String CONTACT_FILE_NAME = "CONTACT_FILE";
    public static final String CONTACTS = "Contact_List";
    public static final String CONTACT_FILE2 = "FALL_UTILS";
    public static final String SWITCH_STATE = "switch_status";

    public SharedPreference() {
        super();
    }

    public void saveContacts(Context context, List<Contact> contacts) {
        SharedPreferences.Editor sharedPref = context.getSharedPreferences(CONTACT_FILE_NAME, Context.MODE_PRIVATE).edit();
        Gson gson = new Gson();

        String jsonFavorites = gson.toJson(contacts);
        sharedPref.putString(CONTACTS, jsonFavorites);
        sharedPref.apply();
    }

    public void addContact(Context context, Contact contact) {
        List<Contact> contacts = getContacts(context);
        if (contacts == null)
            contacts = new ArrayList<Contact>();
        contacts.add(contact);
        saveContacts(context, contacts);
        Log.e("SP add Contact", "contact add " + contact.getName() + " " + contact.getNumber());
    }

    public void removeContact(Context context, Contact contact) {
        ArrayList<Contact> contacts = getContacts(context);
        if (contacts != null) {
            contacts.remove(contact);
            saveContacts(context, contacts);
        }
    }

    public ArrayList<Contact> getContacts(Context context) {
        SharedPreferences settings;
        List<Contact> contacts;

        settings = context.getSharedPreferences(CONTACT_FILE_NAME, Context.MODE_PRIVATE);

        if (settings.contains(CONTACTS)) {
            String jsonString = settings.getString(CONTACTS, null);
            Gson gson = new Gson();
            Contact[] contactArray = gson.fromJson(jsonString, Contact[].class);

            contacts = Arrays.asList(contactArray);
            contacts = new ArrayList<Contact>(contacts);
        } else
            return null;

        return (ArrayList<Contact>) contacts;
    }

    public Boolean contactExist(Context context, Contact contact){
        List<Contact> contacts = getContacts(context);
        if(contacts != null) {
            for (Contact item : contacts) {
                if (item.equals(contact))
                    return true;
            }
            return false;
        }
        else{
            return false;
        }
    }

    public void saveSwitchState(Context context, Boolean state){
        SharedPreferences.Editor sharedPref = context.getSharedPreferences(CONTACT_FILE2, Context.MODE_PRIVATE).edit();
        sharedPref.putBoolean(SWITCH_STATE, state);
        sharedPref.apply();
    }
    public Boolean getSwitchState(Context context){
        SharedPreferences sharedPref = context.getSharedPreferences(CONTACT_FILE2, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(SWITCH_STATE, false);
    }
}