package com.example.luka;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

public class DealActivity extends AppCompatActivity {
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mDatabaseReference;
    private static final int PICTURE_RESULT = 42;
    EditText txtTitle;
    EditText txtDescription;
    EditText txtPrice;
    LukaDeals deal;
    ImageView imageView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deal);
        FirebaseUtil.openFbReference("lukadeals");
        mFirebaseDatabase = FirebaseUtil.mFirebaseDatabase;
        mDatabaseReference = FirebaseUtil.mDatabaseReference;
        txtTitle = (EditText) findViewById(R.id.txtTitle);
        txtPrice = (EditText) findViewById(R.id.txtPrice);
        txtDescription = (EditText) findViewById(R.id.txtDescription);
        imageView = (ImageView) findViewById(R.id.image);
        Intent intent = getIntent();
        LukaDeals deal = (LukaDeals) intent.getSerializableExtra("Deal");
        if (deal == null){
            deal = new LukaDeals();
        }
        this.deal = deal;
        txtTitle.setText(deal.getTitle());
        txtDescription.setText(deal.getDescription());
        txtPrice.setText(deal.getPrice());
        showImage(deal.getImageUrl());
        Button btnImage = findViewById(R.id.btnImage);
        btnImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(intent.createChooser(intent,"Insert Picture"), 42);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.save_menu:
                saveDeal();
                Toast.makeText(this, "Deal saved", Toast.LENGTH_LONG).show();
                clean();
                backToList();
                return true;
            case R.id.delete_menu:
                deleteDeal();
                Toast.makeText(this, "Deal Deleted", Toast.LENGTH_SHORT).show();
                backToList();
            default:
                return super.onOptionsItemSelected(item);

        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.save_menu, menu);
        if (FirebaseUtil.isAdmin) {
            menu.findItem(R.id.delete_menu).setVisible(true);
            menu.findItem(R.id.save_menu).setVisible(true);
            enableEditTexts(true);
            findViewById(R.id.btnImage).setEnabled(true);
        }
        else {
            menu.findItem(R.id.delete_menu).setVisible(false);
            menu.findItem(R.id.save_menu).setVisible(false);
            enableEditTexts(false);
            findViewById(R.id.btnImage).setEnabled(false);

        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICTURE_RESULT && resultCode == RESULT_OK) {
            assert data != null;
            Uri imageUri = data.getData();
            StorageReference ref = FirebaseUtil.mStorageRef.child(imageUri.getLastPathSegment());
            ref.putFile(imageUri).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    String url = taskSnapshot.getStorage().getDownloadUrl().toString();
                    deal.setImageUrl(url);
                    String pictureName = taskSnapshot.getStorage().getPath();
                    deal.setImageName(pictureName);
                    Log.d("Url: ", url);
                    Log.d("Name", pictureName);
                    showImage(url);
                }
            });
        }
    }

    private void saveDeal(){
        deal.setTitle(txtTitle.getText().toString());
        deal.setDescription(txtDescription.getText().toString());
        deal.setPrice(txtPrice.getText().toString());
       if (deal.getId() == null){
           mDatabaseReference.push().setValue(deal);
       }
       else {
           mDatabaseReference.child(deal.getId()).setValue(deal);
       }


    }
    private void deleteDeal (){
        if (deal == null) {
            Toast.makeText(this,"Please save deal before deleting", Toast.LENGTH_LONG).show();
            return;
        }
        mDatabaseReference.child(deal.getId()).removeValue();
        Log.d("image name", deal.getImageName());
        if (deal.getImageName() !=null && deal.getImageName().isEmpty() == false) {
            StorageReference picRef = FirebaseUtil.mStorage.getReference().child(deal.getImageName());
            picRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d("Delete Image", "Image Successfully Deleted");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d("Delete Image", e.getMessage());

                }
            });
        }
    }
    private void backToList() {
        Intent intent = new Intent(this,ListActivity.class);
        startActivity(intent);
    }

    private void clean(){
        txtTitle.setText("");
        txtPrice.setText("");
        txtDescription.setText("");
        txtTitle.requestFocus();

    }
    private void enableEditTexts(boolean isEnabled){
        txtPrice.setEnabled(isEnabled);
        txtDescription.setEnabled(isEnabled);
        txtTitle.setEnabled(isEnabled);
    }
    private void showImage (String url) {
        if (url != null && url.isEmpty() == false) {
            int width = Resources.getSystem().getDisplayMetrics().widthPixels;
            Picasso.with(this).load(url).resize(width, width*2/3).centerCrop().into(imageView);
        }
    }
}