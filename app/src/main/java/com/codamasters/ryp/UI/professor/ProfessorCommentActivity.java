package com.codamasters.ryp.UI.professor;

import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.codamasters.ryp.R;
import com.codamasters.ryp.model.Comment;
import com.codamasters.ryp.utils.adapter.list.comments.CommentListAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfessorCommentActivity extends AppCompatActivity {

    private static final String PREFS = "RYP";

    private ValueEventListener mConnectedListener;
    private CommentListAdapter mCommentListAdapter;
    private String professorKey;
    private String user_name;
    private String user_key;
    private String title;

    private DatabaseReference firebaseRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);



        // Make sure we have a mUsername
        loadUser();

        title = getIntent().getExtras().getString("professor_name", "Comments");
        professorKey = getIntent().getExtras().getString("professor_key", "Error");
        setTitle(title);


        // Setup our Firebase mFirebaseRef
        firebaseRef = FirebaseDatabase.getInstance().getReference().child("chat").child(professorKey);


        // Setup our input methods. Enter key on the keyboard or pushing the send button
        EditText inputText = (EditText) findViewById(R.id.messageInput);
        inputText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_NULL && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    sendMessage();
                }
                return true;
            }
        });

        findViewById(R.id.sendButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });

    }

    @Override
    public void onStart() {
        super.onStart();
        // Setup our view and list adapter. Ensure it scrolls to the bottom as data changes
        final ListView listView = (ListView) findViewById(R.id.chat_lv);
        // Tell our list adapter that we only want 50 messages at a time
        mCommentListAdapter = new CommentListAdapter(this, firebaseRef.limitToFirst(50), this, R.layout.own_chat_message, user_key);
        listView.setAdapter(mCommentListAdapter);
        mCommentListAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                listView.setSelection(mCommentListAdapter.getCount() - 1);
            }
        });

        // Finally, a little indication of connection status
        mConnectedListener = firebaseRef.getRoot().child(".info/connected").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean connected = (Boolean) dataSnapshot.getValue();
                if (connected) {
                    Toast.makeText(ProfessorCommentActivity.this, "Connected", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ProfessorCommentActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError firebaseError) {
                // No-op
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        firebaseRef.getRoot().child(".info/connected").removeEventListener(mConnectedListener);
        mCommentListAdapter.cleanup();
    }

    private void loadUser(){
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        user_name = prefs.getString("user_key", null);
        user_key = prefs.getString("user_key", null);
    }

    private void sendMessage() {

        if(user_name!=null && user_key!=null) {
            EditText inputText = (EditText) findViewById(R.id.messageInput);
            String input = inputText.getText().toString();
            if (!input.equals("")) {
                Comment comment = new Comment(input, user_name, user_key, System.currentTimeMillis());
                firebaseRef.push().setValue(comment);
                inputText.setText("");
            }
        }else{
            Toast.makeText(this, "Sorry, there was an error. Login again.", Toast.LENGTH_SHORT).show();
        }
    }

}