package heycompany.heychat;


import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class ChatsFragment extends Fragment {

    private RecyclerView mConvList;
    private RecyclerView mGroupList;

    private DatabaseReference mConvDatabase;
    private DatabaseReference mMessageDatabase;
    private DatabaseReference mUsersDatabase;
    private DatabaseReference mGroupDatabase;
    private DatabaseReference mGroupMessageDatabase;

    private FirebaseAuth mAuth;

    private String mCurrent_user_id;

    private View mMainView;


    public ChatsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mMainView = inflater.inflate(R.layout.fragment_chats, container, false);

        mConvList = (RecyclerView) mMainView.findViewById(R.id.conv_list);
        mGroupList = (RecyclerView) mMainView.findViewById(R.id.group_list);
        //mGroupList.bringToFront();
        //mGroupList.setZ(1);
        //mConvList.setZ(1);


        mAuth = FirebaseAuth.getInstance();

        mCurrent_user_id = mAuth.getCurrentUser().getUid();

        mGroupDatabase = FirebaseDatabase.getInstance().getReference().child("Groups").child(mCurrent_user_id);

        mConvDatabase = FirebaseDatabase.getInstance().getReference().child("Chat").child(mCurrent_user_id);

        mConvDatabase.keepSynced(true);
        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("Users");
        mMessageDatabase = FirebaseDatabase.getInstance().getReference().child("messages").child(mCurrent_user_id);
        mGroupMessageDatabase = FirebaseDatabase.getInstance().getReference().child("messages");
        mUsersDatabase.keepSynced(true);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);

        mConvList.setHasFixedSize(true);
        mConvList.setLayoutManager(linearLayoutManager);

        LinearLayoutManager GroupManager = new LinearLayoutManager(getContext());
        //GroupManager.setReverseLayout(true);
        GroupManager.setStackFromEnd(true);
        mGroupList.setLayoutManager(GroupManager);


        // Inflate the layout for this fragment
        return mMainView;
    }


    @Override
    public void onStart() {
        super.onStart();

        Query conversationQuery = mConvDatabase.orderByChild("timestamp");
        Query groupsQuery = mGroupDatabase.orderByChild("timestamp");

        FirebaseRecyclerAdapter<Chat, ConvViewHolder> firebaseConvAdapter = new FirebaseRecyclerAdapter<Chat, ConvViewHolder>(
                Chat.class,
                R.layout.users_single_layout,
                ConvViewHolder.class,
                conversationQuery
        ) {
            @Override
            protected void populateViewHolder(final ConvViewHolder convViewHolder, final Chat conv, int i) {


                final String list_user_id = getRef(i).getKey();

                Query lastMessageQuery = mMessageDatabase.child(list_user_id).limitToLast(1);

                lastMessageQuery.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                        String data = dataSnapshot.child("message").getValue().toString();
                        String type = dataSnapshot.child("type").getValue().toString();

                        convViewHolder.setMessage(data, type, conv.isSeen());

                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });


                mUsersDatabase.child(list_user_id).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        final String userName = dataSnapshot.child("name").getValue().toString();
                        String userThumb = dataSnapshot.child("thumb_image").getValue().toString();

                        if(dataSnapshot.hasChild("online")) {

                            String userOnline = dataSnapshot.child("online").getValue().toString();
                            convViewHolder.setUserOnline(userOnline);

                        }

                        convViewHolder.setName(userName);
                        convViewHolder.setUserImage(userThumb, getContext());

                        convViewHolder.mView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {


                                Intent chatIntent = new Intent(getContext(), ChatActivity.class);
                                chatIntent.putExtra("user_id", list_user_id);
                                chatIntent.putExtra("user_name", userName);
                                startActivity(chatIntent);

                            }
                        });

                        convViewHolder.mView.setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View v) {
                                Log.d("Hallo", "test");
                                return true;
                            }
                        });

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }
        };

        mConvList.setAdapter(firebaseConvAdapter);


        FirebaseRecyclerAdapter<Chat, GroupChatViewholder> firebaseGroupAdapter = new FirebaseRecyclerAdapter<Chat, GroupChatViewholder>(
                Chat.class,
                R.layout.users_single_layout,
                GroupChatViewholder.class,
                groupsQuery
        ) {
            @Override
            protected void populateViewHolder(final GroupChatViewholder GroupChatViewholder, final Chat conv, int i) {

                final String list_user_id = getRef(i).getKey();

                Query lastGroupMessageQuery = mGroupMessageDatabase.child(list_user_id).limitToLast(1);

                lastGroupMessageQuery.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                        String data = dataSnapshot.child("message").getValue().toString();
                        String type = dataSnapshot.child("type").getValue().toString();

                        GroupChatViewholder.setMessage(data, type, conv.isSeen());

                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }

                });


                mGroupDatabase.child(list_user_id).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        final String groupName = dataSnapshot.child("groupinfo").child("name").getValue().toString();
                        final String groupID = dataSnapshot.child("groupinfo").child("groupid").getValue().toString();
                        Log.d("hallo", "id:" + groupID);
                        //String userThumb = dataSnapshot.child("thumb_image").getValue().toString();

                        GroupChatViewholder.setName(groupName);
                        //convViewHolder.setUserImage(userThumb, getContext());

                       GroupChatViewholder.groupView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {

                                Log.d("hallo", "geht der hier rein?");

                                Intent groupchatIntent = new Intent(getContext(), GroupChatActivity.class);
                                groupchatIntent.putExtra("user_id", list_user_id);
                                groupchatIntent.putExtra("user_name", groupName);
                                groupchatIntent.putExtra("group_id", groupID);
                                startActivity(groupchatIntent);

                            }
                        });

                        GroupChatViewholder.groupView.setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View v) {
                                Log.d("Hallo", "test");
                                return true;
                            }
                        });

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }
        };

        mGroupList.setAdapter(firebaseGroupAdapter);


    }



    public static class ConvViewHolder extends RecyclerView.ViewHolder {

        View mView;

        public ConvViewHolder(View itemView) {
            super(itemView);

            mView = itemView;

        }


        public void setMessage(String message,String type, boolean isSeen){

            TextView userStatusView = (TextView) mView.findViewById(R.id.user_single_status);
            if (type.equals("image")){
                userStatusView.setText("Bild");
            }
            else if (type.equals("voice")){
                userStatusView.setText("Sprachnachricht");
            }
            else if (type.equals("video")){
                userStatusView.setText("Video");
            }
            else{
                userStatusView.setText(message);
            }


            if(!isSeen){
                userStatusView.setTypeface(userStatusView.getTypeface(), Typeface.BOLD);
            } else {
                userStatusView.setTypeface(userStatusView.getTypeface(), Typeface.NORMAL);
            }

        }

        public void setName(String name){

            TextView userNameView = (TextView) mView.findViewById(R.id.user_single_name);
            userNameView.setText(name);

        }

        public void setUserImage(String thumb_image, Context ctx){

            CircleImageView userImageView = (CircleImageView) mView.findViewById(R.id.user_single_image);
            Picasso.get().load(thumb_image).placeholder(R.drawable.placeholder).into(userImageView);

        }

        public void setUserOnline(String online_status) {

            ImageView userOnlineView = (ImageView) mView.findViewById(R.id.user_single_online_icon);

            if(online_status.equals("true")){

                userOnlineView.setVisibility(View.VISIBLE);

            } else {

                userOnlineView.setVisibility(View.INVISIBLE);

            }

        }

    }

    public static class GroupChatViewholder extends RecyclerView.ViewHolder {

        View groupView;

        public GroupChatViewholder(View itemView) {
            super(itemView);

            groupView = itemView;

        }

        public void setMessage(String message,String type, boolean isSeen){

            TextView userStatusView = (TextView) groupView.findViewById(R.id.user_single_status);
            if (type.equals("image")){
                userStatusView.setText("Bild");
            }
            else if (type.equals("voice")){
                userStatusView.setText("Sprachnachricht");
            }
            else if (type.equals("video")){
                userStatusView.setText("Video");
            }
            else{
                userStatusView.setText(message);
            }


            if(!isSeen){
                userStatusView.setTypeface(userStatusView.getTypeface(), Typeface.BOLD);
            } else {
                userStatusView.setTypeface(userStatusView.getTypeface(), Typeface.NORMAL);
            }

        }

        public void setName(String name){

            TextView userNameView = (TextView) groupView.findViewById(R.id.user_single_name);
            userNameView.setText(name);

        }

        public void setUserImage(String thumb_image, Context ctx){

            CircleImageView userImageView = (CircleImageView) groupView.findViewById(R.id.user_single_image);
            Picasso.get().load(thumb_image).placeholder(R.drawable.placeholder).into(userImageView);

        }

    }


}