package com.alessandro.falldetector;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.LocationManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import java.util.ArrayList;

import com.alessandro.falldetector.utils.Contact;
import com.alessandro.falldetector.utils.SharedPreference;


public class MainActivity extends AppCompatActivity {

    private static final int CONTACT_PICKER_RESULT = 10009;
    private String TAG = this.getClass().getName();

    private static final int PICK_CONTACT_REQUEST = 1;

    private ArrayList<Contact> m_contactsList;
    private RecyclerView.Adapter mAdapter;

    private Switch detectionServiceSwitch;

    SharedPreference sharedPreference;
    private FloatingActionButton addContactButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        Log.e("Main Activity", "OnCreate()");


        Toolbar toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Fall Detector");

        //RecyclerView for contacts list
        sharedPreference = new SharedPreference();
        m_contactsList = sharedPreference.getContacts(this);

        if (m_contactsList == null)
            m_contactsList = new ArrayList<>();

        mAdapter = new ContactListAdapter(m_contactsList, this);
        RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.recyclerList);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter);

        //Floating Action Button - FAB
        addContactButton = (FloatingActionButton) findViewById(R.id.add_contact_button);
        addContactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //pick name + number
                //Intent pick_contact = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                //startActivityForResult(pick_contact, PICK_CONTACT_REQUEST);

                //pick email
                Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Email.CONTENT_URI);
                contactPickerIntent.setType(ContactsContract.CommonDataKinds.Email.CONTENT_TYPE);
                startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
            }
        });

        //Switch for fall detection
        detectionServiceSwitch = (Switch) findViewById(R.id.detection_switch);
        detectionServiceSwitch.setChecked(sharedPreference.getSwitchState(getApplicationContext()));

        detectionServiceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (m_contactsList.isEmpty() && isChecked) {
                    isChecked = false;
                    createAlertDialog();
                    detectionServiceSwitch.setChecked(isChecked);
                }
                if (isChecked)
                    startDetectionService();
                else
                    stopDetectionService();
                sharedPreference.saveSwitchState(getApplicationContext(), isChecked);
            }
        });
    }

    private void createAlertDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Contacts list is Empty");
        dialog.setMessage("Please add at least one contact");

        dialog.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Email.CONTENT_URI);
                contactPickerIntent.setType(ContactsContract.CommonDataKinds.Email.CONTENT_TYPE);
                startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
            }
        });
        dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                return;
            }
        });

        AlertDialog noContactsDialog = dialog.create();
        noContactsDialog.show();
    }

    //pick contact name and number
/*    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_CONTACT_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                Uri contactData = data.getData();
                Cursor c = getContentResolver().query(contactData, null, null, null, null);

                if (c.moveToFirst()) {
                    String id = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID));

                    String hasPhone = c.getString(c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));

                    if (hasPhone.equalsIgnoreCase("1")) {
                        Cursor phones = getContentResolver().query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + id,
                                null, null);
                        phones.moveToFirst();
                        String phn_no = phones.getString(phones.getColumnIndex("data1"));
                        String name = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.DISPLAY_NAME));

                        Toast.makeText(getApplicationContext(), "contact info : " + phn_no + "\n" + name, Toast.LENGTH_LONG).show();
                        Log.e("SettingFragment", "ContactPicked" + phn_no + " " + name);
                        saveContactPreference(name, phn_no);

                    } else {
                        Toast.makeText(getApplicationContext(), "This contact haven't a telephone number...", Toast.LENGTH_LONG).show();
                    }
                }

            }
        }
    }*/


    //pick email name and email

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK)

        {
            switch (requestCode) {
                case CONTACT_PICKER_RESULT:
                    Cursor cursor = null;
                    String email = "", name = "";
                    try {
                        Uri result = data.getData();
                        Log.v(TAG, "Got a contact result: " + result.toString());

                        // get the contact id from the Uri
                        String id = result.getLastPathSegment();

                        // query for everything email
                        cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, ContactsContract.CommonDataKinds.Email._ID + "=" + id, null, null);

                        int nameId = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                        int emailIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA);

                        // let's just get the first email
                        if (cursor.moveToFirst()) {
                            email = cursor.getString(emailIdx);
                            name = cursor.getString(nameId);
                            Log.v(TAG, "Got email: " + email);
                        } else {
                            Log.w(TAG, "No results");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to get email data", e);
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                        saveContactPreference(name, email);
                        if (email.length() == 0 && name.length() == 0) {
                            Toast.makeText(this, "No Email for Selected Contact", Toast.LENGTH_LONG).show();
                        } else {
                            saveContactPreference(name, email);
                        }
                    }
                    break;
            }
        } else {
            Log.w(TAG, "Warning: activity result not ok");
        }
    }


    private void saveContactPreference(String name, String number) {

        Contact contact = new Contact(name, number);
        if (sharedPreference.contactExist(this, contact))
            return;
        m_contactsList.add(contact);
        sharedPreference.addContact(this, contact);
        mAdapter.notifyDataSetChanged();

        Log.e("Main Activity", "Contact stored in the list");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e("Main Activity", "OnDestroy()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        detectionServiceSwitch.setChecked(sharedPreference.getSwitchState(getApplicationContext()));
        Log.e("Main Activity", "OnResume()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e("Main Activity", "OnPause()");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingActivity.class));
            return true;
        }
        if (id == R.id.action_help) {
            createHelpDialog(1);
        }
        return super.onOptionsItemSelected(item);
    }

    private void createHelpDialog(int step) {
        switch (step) {
            case 1:
                AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                dialog.setTitle("Fall Detector Help");
                dialog.setMessage("Fall Detector is an application built to detect falls. This help explains how to use the key features");

                dialog.setPositiveButton("Next", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        createHelpDialog(2);
                    }
                });
                dialog.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        return;
                    }
                });

                AlertDialog noContactsDialog = dialog.create();
                noContactsDialog.show();
                break;
            case 2:
                AlertDialog.Builder dialog2 = new AlertDialog.Builder(this);
                dialog2.setTitle("Settings");
                dialog2.setIcon(R.drawable.ic_settings_blue_24dp);
                dialog2.setMessage("Click the settings icon and customize the alarms notification settings and the location setting");

                dialog2.setPositiveButton("Next", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        createHelpDialog(3);
                    }
                });
                dialog2.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        return;
                    }
                });

                AlertDialog helpDialog2 = dialog2.create();
                helpDialog2.show();
                break;
            case 3:
                AlertDialog.Builder dialog3 = new AlertDialog.Builder(this);
                dialog3.setTitle("Add contact button");
                dialog3.setIcon(R.drawable.ic_person_add_blue_24dp);
                dialog3.setMessage("Click the add button to add a contact to the emergency contact list, this list will be later used to deliver the emergency message by e-mail.");

                dialog3.setPositiveButton("Next", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        createHelpDialog(4);
                    }
                });
                dialog3.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        return;
                    }
                });

                AlertDialog helpDialog3 = dialog3.create();
                helpDialog3.show();
                break;
            case 4:
                AlertDialog.Builder dialog4 = new AlertDialog.Builder(this);
                dialog4.setTitle("Fall detection service");
                dialog4.setMessage("Click the toggle button to enable or disable the fall detection system");

                dialog4.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        return;
                    }
                });

                AlertDialog helpDialog4 = dialog4.create();
                helpDialog4.show();
                break;
        }
    }

    private void stopDetectionService() {
        stopService(new Intent(MainActivity.this, SensorService.class));
    }

    private void startDetectionService() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean userWantLocation = sharedPreferences.getBoolean("pref_location_enabled", false);

        if (userWantLocation) {

            LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

                startService(new Intent(MainActivity.this, SensorService.class));

            } else {
                AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                dialog.setTitle("Gps is disabled");
                dialog.setMessage("Enable the GPS to get the user location, or go to settings and disable the location option");

                dialog.setNeutralButton("Setting", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(getApplicationContext(), SettingActivity.class));
                    }
                });
                dialog.setPositiveButton("Enable GPS", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent viewIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(viewIntent);
                    }
                });

                AlertDialog gpsDialog = dialog.create();
                gpsDialog.show();
            }
        } else {
            startService(new Intent(MainActivity.this, SensorService.class));
        }

    }
}

