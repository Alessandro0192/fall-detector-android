package com.alessandro.falldetector;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.alessandro.falldetector.utils.Contact;
import com.alessandro.falldetector.utils.SharedPreference;

import java.util.List;

public class ContactListAdapter extends RecyclerView.Adapter<ContactListAdapter.ViewHolder> {
    List<Contact> contacts;
    private Context context;
    SharedPreference sharedPreference;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView contectNameTxt;
        public TextView contactNumberTxt;
        public ImageButton contactRemoveButton;

        public ViewHolder(View v) {
            super(v);
            contectNameTxt = (TextView) v.findViewById(R.id.contactName);
            contactNumberTxt = (TextView) v.findViewById(R.id.contactNumber);
            contactRemoveButton = (ImageButton) v.findViewById(R.id.removeButton);
        }
    }


    public ContactListAdapter(List<Contact> myDataset, Context context) {
        contacts = myDataset;
        this.context = context;
        sharedPreference = new SharedPreference();
    }


    @Override
    public ContactListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.contact_card, parent, false);
        return new ViewHolder(v);
    }

    // Replace the contents of a view
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        Contact c =contacts.get(position);
        holder.contectNameTxt.setText(c.getName());
        holder.contactNumberTxt.setText(c.getNumber());
        holder.contactRemoveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Contact c = contacts.get(position);
                sharedPreference.removeContact(context, c);
                contacts.remove(position);
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }


}