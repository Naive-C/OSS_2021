package project.oss_2021;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import project.oss_2021.Cards.arrayAdapter;
import project.oss_2021.Cards.cards;
import project.oss_2021.Matches.MatchesActivity;
import project.oss_2021.Choice.ChoiceActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.lorentzos.flingswipe.SwipeFlingAdapterView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;

import java.util.Locale;
import java.util.Map;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private cards cards_data[];
    private project.oss_2021.Cards.arrayAdapter arrayAdapter;
    private int distance;
    private FirebaseAuth mAuth;
    private String currentUId;
    private DatabaseReference usersDb;

    private GpsTracker gpsTracker;

    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    String[] REQUIRED_PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

    private String university;
    private double latitude;
    private double longitude;

    private Button mSignout, mSetting, mChoice, mMatches;

    ListView listView;
    List<cards> rowItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        usersDb = FirebaseDatabase.getInstance().getReference().child("Users");
        mAuth = FirebaseAuth.getInstance();
        currentUId = mAuth.getCurrentUser().getUid();

        checkUserInfo();

        mSignout = findViewById(R.id.signout);
        mSetting = findViewById(R.id.setting);
        mChoice = findViewById(R.id.choice);
        mMatches = findViewById(R.id.matches);
        mSignout.setOnClickListener(view -> {
            mAuth.signOut();
            Intent intent = new Intent(MainActivity.this, StartActivity.class); // MainActivity -> ChooseLoginRegistrationActivity
            startActivity(intent);
            finish();
            return;
        });
        mSetting.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return;
        });
        mChoice.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, ChoiceActivity.class);
            startActivity(intent);
            return;
        });
        mMatches.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, MatchesActivity.class);
            startActivity(intent);
            return;
        });


        gpsTracker = new GpsTracker(MainActivity.this);

        latitude = gpsTracker.getLatitude();
        longitude = gpsTracker.getLongitude();

        String address = getCurrentAddress(latitude, longitude);

        Map userInfo = new HashMap();
        userInfo.put("latitude", latitude);
        userInfo.put("longitude", longitude);
        usersDb.child(currentUId).updateChildren(userInfo);


        rowItems = new ArrayList<cards>(); // ?????? ?????????
        arrayAdapter = new arrayAdapter(this, R.layout.item, rowItems); //???????????? al??? ???????????? ??????, item??? ????????????, ??????
        //al.add("java") <- ????????? ????????? ????????? ?????? ??? ????????? ????????? ????????????
        //arrayAdapter.notifyDataSetChanged(); <- ?????????????????? ?????? ?????????
        SwipeFlingAdapterView flingContainer = findViewById(R.id.frame);
        flingContainer.setAdapter(arrayAdapter);
        flingContainer.setFlingListener(new SwipeFlingAdapterView.onFlingListener() {
            // ????????? ??????
            @Override
            public void removeFirstObjectInAdapter() {
                // this is the simplest way to delete an object from the Adapter (/AdapterView)
                Log.d("LIST", "removed object!");
                rowItems.remove(0);
                arrayAdapter.notifyDataSetChanged();
            }

            //????????? ???????????? ?????? ???
            @Override
            public void onLeftCardExit(Object dataObject) {
                cards object = (cards) dataObject;
                String userId = object.getUserId();
                usersDb.child(userId).child("connection").child("nope").child(currentUId).setValue(true); //???????????? ????????? connection ?????? -> nope ????????? ?????? true
                Toast.makeText(MainActivity.this, "Nope!", Toast.LENGTH_SHORT).show();
            }

            //????????? ??????????????? ?????? ???
            @Override
            public void onRightCardExit(Object dataObject) {
                cards object = (cards) dataObject;
                String userId = object.getUserId();
                usersDb.child(userId).child("connection").child("like").child(currentUId).setValue(true); //??????????????? ????????? connection ?????? -> like ?????? -> ?????? ????????? ?????? ?????? true
                isConnectionMatch(userId);
                Toast.makeText(MainActivity.this, "Like!", Toast.LENGTH_SHORT).show();
            }

            //????????? ??? ?????? ????????? ???
            @Override
            public void onAdapterAboutToEmpty(int itemsInAdapter) {
            }

            @Override
            public void onScroll(float scrollProgressPercent) {
            }
        });
        // Optionally add an OnItemClickListener
        flingContainer.setOnItemClickListener(new SwipeFlingAdapterView.OnItemClickListener() {
            @Override
            public void onItemClicked(int itemPosition, Object dataObject) {
                Toast.makeText(MainActivity.this, "Click!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //?????? ??????
    private void isConnectionMatch(String userId) {
        DatabaseReference currentUserConnectionsDb = usersDb.child(currentUId).child("connection").child("like").child(userId); // ????????? like??? ?????? ??????
        currentUserConnectionsDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(MainActivity.this, "new Connection", Toast.LENGTH_SHORT).show();
                    String key = FirebaseDatabase.getInstance().getReference().child("Chat").push().getKey();
                    usersDb.child(snapshot.getKey()).child("connection").child("choice").child(currentUId).setValue(null);
                    usersDb.child(currentUId).child("connection").child("choice").child(snapshot.getKey()).setValue(null);
                    usersDb.child(snapshot.getKey()).child("connection").child("matches").child(currentUId).child("ChatId").setValue(key); //"???"??? matches ?????? -> "????????? like??? ?????? ??????"??? ?????? -> ChatId ??????
                    usersDb.child(currentUId).child("connection").child("matches").child(snapshot.getKey()).child("ChatId").setValue(key); //"???"?????? like??? ?????? ??????"??? matches ?????? -> "???"??? ?????? -> ChatId ??????
                    // usersDb.child(snapshot.getKey()).child("connection").child("matches").child(currentUId).setValue(true);  //"???"??? matches ????????? "????????? like??? ?????? ??????"??? ?????? ?????? true
                    // usersDb.child(currentUId).child("connection").child("matches").child(snapshot.getKey()).setValue(true);  //"????????? like??? ?????? ??????"??? matches ????????? "???"??? ?????? ?????? true
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private String userSex; // ???????????? ??????
    private String userSexOpp; // ???????????? ?????? ??????
    private String hobby1, hobby2, hobby3, purpose;
    private boolean hobbyCheck, purposeCheck;


    public void checkUserInfo() {
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // userDb??? ??????????????????
        DatabaseReference userDb = usersDb.child(user.getUid());
        userDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getKey().equals(user.getUid())) {
                    if (snapshot.exists()) {
                        Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                        if(map.get("hobby1")!=null){
                            hobby1 = map.get("hobby1").toString();

                        }
                        if(map.get("hobby2")!=null){
                            hobby2 = map.get("hobby2").toString();

                        }
                        if(map.get("hobby3")!=null){
                            hobby3 = map.get("hobby3").toString();

                        }
                        if(map.get("hobbyCheck")!=null){
                            hobbyCheck = Boolean.parseBoolean(map.get("hobbyCheck").toString());

                        }
                        if(map.get("purposeCheck")!=null){
                            purposeCheck = Boolean.parseBoolean(map.get("purposeCheck").toString());

                        }
                        if(map.get("purpose")!=null){
                            purpose = map.get("purpose").toString();

                        }
                        if(map.get("distance")!=null){
                            String dis = map.get("distance").toString();
                            System.out.println(dis);
                            distance = Integer.parseInt(dis);
                        }


                        if (snapshot.child("sex").getValue() != null) {
                            userSex = snapshot.child("sex").getValue().toString();
                            switch (userSex) {
                                case "Male":
                                    userSexOpp = "Female";
                                    break;
                                case "Female":
                                    userSexOpp = "Male";
                                    break;
                            }
                            getOppositeSexUsers();
                        }
                        if (snapshot.child("university").getValue() != null) {
                            university = snapshot.child("university").getValue().toString();

                        }


                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }


    public boolean checkDistance(String t1, String t2, int distance) {
        long latitudeOpp = Long.parseLong(t1);
        long longitudeOpp = Long.parseLong(t2);


        if (Math.sqrt(Math.pow((latitudeOpp - latitude), 2) + Math.pow((longitudeOpp - longitude), 2)) < distance) {
            return true;
        } else {
            return false;
        }


    }

    //?????? ????????? ????????????
    public void getOppositeSexUsers() {
        usersDb.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                //????????? ?????????
                if (snapshot.child("sex").getValue() != null) {
                    if (snapshot.exists() && !snapshot.child("connection").child("nope").hasChild(currentUId) && !snapshot.child("connection").child("like").hasChild(currentUId) && snapshot.child("sex").getValue().toString().equals(userSexOpp) && snapshot.child("university").getValue().toString().equals(university)) {
                        if (checkDistance(snapshot.child("latitude").getValue().toString(), snapshot.child("longitude").getValue().toString(), distance)) {   //?????? ?????????
                            if(hobbyCheck){
                                String [] hobbys = {hobby1, hobby2, hobby3};
                                if(Arrays.asList(hobbys).contains(snapshot.child("hobby1").getValue().toString()) || Arrays.asList(hobbys).contains(snapshot.child("hobby2").getValue().toString()) || Arrays.asList(hobbys).contains(snapshot.child("hobby3").getValue().toString())){
                                    if(purposeCheck){
                                        if(purpose.equals(snapshot.child("purpose").getValue().toString())){
                                            String profileImageUrl = "default";
                                            if (!snapshot.child("profileImageUrl").getValue().equals("default")) {
                                                profileImageUrl = snapshot.child("profileImageUrl").getValue().toString();
                                            }
                                            cards item = new cards(snapshot.getKey(), snapshot.child("name").getValue().toString(), profileImageUrl);
                                            rowItems.add(item);
                                            arrayAdapter.notifyDataSetChanged();
                                        }


                                    }
                                    else{
                                        String profileImageUrl = "default";
                                        if (!snapshot.child("profileImageUrl").getValue().equals("default")) {
                                            profileImageUrl = snapshot.child("profileImageUrl").getValue().toString();
                                        }
                                        cards item = new cards(snapshot.getKey(), snapshot.child("name").getValue().toString(), profileImageUrl);
                                        rowItems.add(item);
                                        arrayAdapter.notifyDataSetChanged();
                                    }



                                }
                            }else{
                                if(purposeCheck){
                                    if(purpose.equals(snapshot.child("purpose").getValue().toString())){
                                        String profileImageUrl = "default";
                                        if (!snapshot.child("profileImageUrl").getValue().equals("default")) {
                                            profileImageUrl = snapshot.child("profileImageUrl").getValue().toString();
                                        }
                                        cards item = new cards(snapshot.getKey(), snapshot.child("name").getValue().toString(), profileImageUrl);
                                        rowItems.add(item);
                                        arrayAdapter.notifyDataSetChanged();
                                    }


                                }
                                else{
                                    String profileImageUrl = "default";
                                    if (!snapshot.child("profileImageUrl").getValue().equals("default")) {
                                        profileImageUrl = snapshot.child("profileImageUrl").getValue().toString();
                                    }
                                    cards item = new cards(snapshot.getKey(), snapshot.child("name").getValue().toString(), profileImageUrl);
                                    rowItems.add(item);
                                    arrayAdapter.notifyDataSetChanged();
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }
    // -----------????????? ?????? gps??????-----------------

    public void onRequestPermissionsResult(int permsRequestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grandResults) {

        super.onRequestPermissionsResult(permsRequestCode, permissions, grandResults); //?????? ?????? ????????? ??????????????? ???????????? ?????? ??????
        if (permsRequestCode == PERMISSIONS_REQUEST_CODE && grandResults.length == REQUIRED_PERMISSIONS.length) {

            // ?????? ????????? PERMISSIONS_REQUEST_CODE ??????, ????????? ????????? ???????????? ??????????????????

            boolean check_result = true;


            // ?????? ???????????? ??????????????? ???????????????.

            for (int result : grandResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }


            if (check_result) {

                //?????? ?????? ????????? ??? ??????
                ;
            } else {
                // ????????? ???????????? ????????? ?????? ????????? ??? ?????? ????????? ??????????????? ?????? ???????????????.2 ?????? ????????? ????????????.

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])) {

                    Toast.makeText(MainActivity.this, "???????????? ?????????????????????. ?????? ?????? ???????????? ???????????? ??????????????????.", Toast.LENGTH_LONG).show();
                    finish();


                } else {

                    Toast.makeText(MainActivity.this, "???????????? ?????????????????????. ??????(??? ??????)?????? ???????????? ???????????? ?????????. ", Toast.LENGTH_LONG).show();

                }
            }

        }
    }

    void checkRunTimePermission() {

        //????????? ????????? ??????
        // 1. ?????? ???????????? ????????? ????????? ???????????????.
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION);


        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {

            // 2. ?????? ???????????? ????????? ?????????
            // ( ??????????????? 6.0 ?????? ????????? ????????? ???????????? ???????????? ????????? ?????? ????????? ?????? ???????????????.)


            // 3.  ?????? ?????? ????????? ??? ??????


        } else {  //2. ????????? ????????? ????????? ?????? ????????? ????????? ????????? ???????????????. 2?????? ??????(3-1, 4-1)??? ????????????.

            // 3-1. ???????????? ????????? ????????? ??? ?????? ?????? ????????????
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, REQUIRED_PERMISSIONS[0])) {

                // 3-2. ????????? ???????????? ?????? ?????????????????? ???????????? ????????? ????????? ???????????? ????????? ????????????.
                Toast.makeText(MainActivity.this, "??? ?????? ??????????????? ?????? ?????? ????????? ???????????????.", Toast.LENGTH_LONG).show();
                // 3-3. ??????????????? ????????? ????????? ?????????. ?????? ????????? onRequestPermissionResult?????? ???????????????.
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);


            } else {
                // 4-1. ???????????? ????????? ????????? ??? ?????? ?????? ???????????? ????????? ????????? ?????? ?????????.
                // ?????? ????????? onRequestPermissionResult?????? ???????????????.
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            }

        }

    }


    public String getCurrentAddress(double latitude, double longitude) {

        //????????????... GPS??? ????????? ??????
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        List<Address> addresses;

        try {

            addresses = geocoder.getFromLocation(
                    latitude,
                    longitude,
                    7);
        } catch (IOException ioException) {
            //???????????? ??????
            Toast.makeText(this, "???????????? ????????? ????????????", Toast.LENGTH_LONG).show();
            return "???????????? ????????? ????????????";
        } catch (IllegalArgumentException illegalArgumentException) {
            Toast.makeText(this, "????????? GPS ??????", Toast.LENGTH_LONG).show();
            return "????????? GPS ??????";

        }


        if (addresses == null || addresses.size() == 0) {
            Toast.makeText(this, "?????? ?????????", Toast.LENGTH_LONG).show();
            return "?????? ?????????";

        }

        Address address = addresses.get(0);
        return address.getAddressLine(0).toString() + "\n";

    }


    //??????????????? GPS ???????????? ?????? ????????????
    private void showDialogForLocationServiceSetting() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("?????? ????????? ????????????");
        builder.setMessage("?????? ???????????? ???????????? ?????? ???????????? ???????????????.\n"
                + "?????? ????????? ???????????????????");
        builder.setCancelable(true);
        builder.setPositiveButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case GPS_ENABLE_REQUEST_CODE:

                //???????????? GPS ?????? ???????????? ??????
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {

                        Log.d("@@@", "onActivityResult : GPS ????????? ?????????");
                        checkRunTimePermission();
                        return;
                    }
                }

                break;
        }
    }

    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }
}