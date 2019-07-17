package com.metzner.enrico.epilepsy.users;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.metzner.enrico.epilepsy.R;
import com.metzner.enrico.epilepsy.epi_tools.PermissionsHelper;
import com.metzner.enrico.epilepsy.epi_tools.SeizureEntryHelper;
import com.metzner.enrico.epilepsy.epi_tools.UserEntryHelper;
import com.metzner.enrico.epilepsy.seizure.SeizureHolder;
import com.metzner.enrico.epilepsy.settings.LocaleManager;
import com.metzner.enrico.epilepsy.settings.Settings;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

public class UserOverview extends AppCompatActivity {

    private static final String TAG = "USER_OVERVIEW_ACTIVITY";

    private Context context;
    private ListView userList;




    /**********************************************************************************************
     **** CONFIGURATION ***************************************************************************
     **********************************************************************************************/
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.setLocale(newBase));
        Log.d(TAG, "attachBaseContext");
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LocaleManager.setLocale(this);
        Log.d(TAG, "onConfigurationChanged: "+newConfig.locale.getLanguage());
    }




    /**********************************************************************************************
     **** MENU ************************************************************************************
     **********************************************************************************************/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_user, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.user_menu_create:
                Intent newUserIntent = new Intent(this, NewUser.class);
                newUserIntent.putExtra(NewUser.ENTRYSTATE_EXTRA_ID, NewUser.ENTRYSTATE_NEW);
                startActivity(newUserIntent);
                return true;
            case R.id.user_menu_settings:
                Intent settingIntent = new Intent(this, Settings.class);
                startActivity(settingIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }




    /**********************************************************************************************
     **** INITIALISATION **************************************************************************
     **********************************************************************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocaleManager.resetTitle(this);
        setContentView(R.layout.activity_user_overview);

        context = this;

        userList = (ListView) findViewById(R.id.user_list_view);
        userList.setOnItemClickListener(new UsersListener());
    }
    @Override
    protected void onResume() {
        super.onResume();

        ArrayList<String> users = UserEntryHelper.loadUsers(this);
        if( PermissionsHelper.isPermissionGranted(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                && users.size()<1 ) {
            final View noUserView = findViewById(R.id.user_overview_no_user_overlay);
            noUserView.setVisibility(View.VISIBLE);
            final Button createButton = (Button) findViewById(R.id.user_overview_no_user_create);
            createButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    noUserView.setVisibility(View.INVISIBLE);
                    Intent newUserIntent = new Intent(context, NewUser.class);
                    newUserIntent.putExtra(NewUser.ENTRYSTATE_EXTRA_ID, NewUser.ENTRYSTATE_NEW);
                    startActivity(newUserIntent);
                }
            });
            Button laterButton = (Button) findViewById(R.id.user_overview_no_user_later);
            laterButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    noUserView.setVisibility(View.INVISIBLE);
                }
            });
        }

        fillUserList();
    }




    /**********************************************************************************************
     **** ENTRIES WORKAROUND **********************************************************************
     **********************************************************************************************/
    private void fillUserList() {
        Log.d(TAG, "fillUserList: Fill list...");
        ArrayList<String> data;
        if ( PermissionsHelper.isPermissionGranted(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ) {
            data = UserEntryHelper.loadUsers(this);
        } else {
            data = new ArrayList<>();
            data.clear();
        }

        ArrayList<UserHolder> userListItems = new ArrayList<>();
        userListItems.clear();
        for (String s : data) {
            UserHolder temp_user_holder = new UserHolder(this, s);
            userListItems.add(temp_user_holder);
        }
        if(userListItems.size()==0) {
            userListItems.add(new UserHolder(this, UserEntryHelper.UserEntryFillString));
        }
        userList.setAdapter(new UsersAdapter(this, userListItems));
    }
    private String timeSinceUsersLastSeizure(@Nullable String userDirName) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int month2 = (month+11)%12;
        int year2 = (month2>month ? year-1 : year);
        ArrayList<String> seizures = SeizureEntryHelper.loadSeizureList(this, userDirName, year, month);
        seizures.addAll(SeizureEntryHelper.loadSeizureList(this, userDirName, year2, month2));


        Collections.sort(seizures);
        long timeDiff;
        if (seizures.size() > 0) {
            SeizureHolder lastSeizure = new SeizureHolder(this, seizures.get(seizures.size() - 1));
            int[] lastSeizureDate = lastSeizure.getSeizureDate();
            int[] lastSeizureTime = lastSeizure.getSeizureTime();
            Date currentDate = new Date(System.currentTimeMillis());
            Calendar lastSeizureEntry = Calendar.getInstance();
            lastSeizureEntry.set(lastSeizureDate[0], lastSeizureDate[1], lastSeizureDate[2],
                    lastSeizureTime[0], lastSeizureTime[1]);
            timeDiff = (currentDate.getTime() - lastSeizureEntry.getTimeInMillis()) / 1000L;
            timeDiff = (timeDiff + 30L) / 60L;
        } else {
            timeDiff = 30L * 1440L;
        }
        int timeElapsed_minutes = (int) (timeDiff % 60L);
        int timeElapsed_hours = (int) ((timeDiff / 60L) % 24L);
        int timeElapsed_days = (int) (timeDiff / 1440L);
        String elapsedTimeString = "";
        if (timeElapsed_days > 0)
            elapsedTimeString = timeElapsed_days + " " + getResources().getString(R.string.elapsed_time_days) + " ";
        if (seizures.size() > 0) {
            if (timeElapsed_hours > 0 || timeElapsed_days > 0)
                elapsedTimeString += timeElapsed_hours + " " + getResources().getString(R.string.elapsed_time_hours) + " ";
            if (timeElapsed_days < 1)
                elapsedTimeString += timeElapsed_minutes + " " + getResources().getString(R.string.elapsed_time_minutes);
        } else {
            elapsedTimeString = ">" + elapsedTimeString;
        }
        elapsedTimeString += " "+getResources().getString(R.string.elapsed_time_since);
        return elapsedTimeString;
    }

    private class UserViewHolder {
        ImageView symbol;
        TextView tv_seizure_type;
        TextView tv_description;
    }
    @SuppressWarnings("CanBeFinal")
    private class UsersAdapter extends BaseAdapter {
        private Context context;
        private ArrayList<UserHolder> listOfUsers;

        UsersAdapter(Context _context, ArrayList<UserHolder> entries) {
            context = _context;
            listOfUsers = entries;
        }

        @Override
        public int getViewTypeCount() {
            return listOfUsers.size();
        }
        @Override
        public int getItemViewType(int position) {
            return position;
        }
        @Override
        public int getCount() {
            return listOfUsers.size();
        }
        @Override
        public Object getItem(int position) {
            return listOfUsers.get(position);
        }
        @Override
        public long getItemId(int position) {
            return 0;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final UserViewHolder holder;

            if (convertView == null) {
                holder = new UserViewHolder();
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                assert inflater != null;
                convertView = inflater.inflate(R.layout.imgtv_informativ_entry, null, true);

                holder.symbol = (ImageView) convertView.findViewById(R.id.imgtv_informative_entry_symbol);
                holder.tv_seizure_type = (TextView) convertView.findViewById(R.id.imgtv_informative_entry_title_title);
                holder.tv_description = (TextView) convertView.findViewById(R.id.imgtv_informative_entry_description);

                convertView.setTag(holder);
            }else {
                // the getTag returns the viewHolder object set as a tag to the view
                holder = (UserViewHolder)convertView.getTag();
            }

            //get content of diary entry
            UserHolder user_entry = listOfUsers.get(position);
            String userName = getResources().getString(R.string.diary_no_data);
            String userEntryDescriptionText = "...";
            int diary_symbel_imageID = R.drawable.seizuretype_unknown;
            if(!user_entry.getUserEntryString().equals(UserEntryHelper.UserEntryFillString)) {
                userName = user_entry.getName() + "  (" + user_entry.getUserDir().getName() + ")";
                userEntryDescriptionText = timeSinceUsersLastSeizure(user_entry.getUserDir().getName());
                //diary_symbel_imageID = 0;
            }

            //set content of diary entry
            holder.symbol.setImageDrawable(getDrawable(diary_symbel_imageID));
            holder.tv_seizure_type.setText(userName);
            holder.tv_description.setText(userEntryDescriptionText);

            return convertView;
        }
    }
    private class UsersListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            //System.out.println("Clicked on item: "+position+" of possible 0-"+(parent.getCount()-1));
            UserHolder entry = (UserHolder) parent.getAdapter().getItem(position);
            if(entry.getUserEntryString().equals(UserEntryHelper.UserEntryFillString))
                return;

            Intent editorIntent = new Intent(parent.getContext(), NewUser.class);
            editorIntent.putExtra(NewUser.ENTRYSTRING_EXTRA_ID, entry.getUserEntryString());
            editorIntent.putExtra(NewUser.ENTRYSTATE_EXTRA_ID, NewUser.ENTRYSTATE_EDIT);
            startActivity(editorIntent);
        }
    }




    /**********************************************************************************************
     **** ADD USER ********************************************************************************
     **********************************************************************************************/
    public void addUser(View view) {
        Intent editorIntent = new Intent(this, NewUser.class);
        editorIntent.putExtra(NewUser.ENTRYSTATE_EXTRA_ID, NewUser.ENTRYSTATE_NEW);
        startActivity(editorIntent);
    }
}
