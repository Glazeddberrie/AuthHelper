package com.glazeddberrie.authhelper.Model;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;


import com.glazeddberrie.authhelper.R;

import java.util.ArrayList;

public class PasswordAdapter extends ArrayAdapter<Password> {
    private Context context;
    private ArrayList<Password> passwordsList;

    public PasswordAdapter(Context context, ArrayList<Password> passwordsList) {
        super(context, 0, passwordsList);
        this.context = context;
        this.passwordsList = passwordsList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // If no view is available, create a new one
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.password_item_layout, parent, false);
        }

        // Get the current password item
        Password currentItem = passwordsList.get(position);

        // Find the TextViews for service name and user-assigned name
        TextView serviceNameTextView = convertView.findViewById(R.id.user_assigned_name);
        TextView userAssignedNameTextView = convertView.findViewById(R.id.service_name);

        // Set the text for each TextView
        serviceNameTextView.setText(currentItem.getServiceName());
        userAssignedNameTextView.setText(currentItem.getUserAssignedName());

        return convertView;
    }
}