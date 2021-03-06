package com.capstone.project.trashhub

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.capstone.project.trashhub.data.PROFILE_NAME
import com.capstone.project.trashhub.databinding.ActivityProfileBinding
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.io.ByteArrayOutputStream
import java.lang.Exception

class ProfileActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var imageUri: Uri
    private lateinit var auth: FirebaseAuth
    private lateinit var firebaseFirestore: FirebaseFirestore
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        auth = FirebaseAuth.getInstance()
        firebaseFirestore = FirebaseFirestore.getInstance()

        binding.btnBack.setOnClickListener(this)
        setupAction()
    }

    private fun setupAction() {
        val user = auth.currentUser
        val name = binding.nameEdtText.text.toString()
        val alamat = binding.alamatEdtText.text.toString()
        val jenisKelamin = binding.jenisKelaminEdtText.text.toString()
        val noHp = binding.noHpEdtText.text.toString()
//        val dataSnapshot =

        if(user != null){
            if (user.photoUrl != null){
                Picasso.get().load(user.photoUrl).into(binding.imgProfile)
            }else{
                Picasso.get().load(R.drawable.ic_baseline_person_50).into(binding.imgProfile)
            }

            binding.nameEdtText.setText(user.displayName)
            binding.emailEdtText.setText(user.email)
            firebaseFirestore.collection(PROFILE_NAME).whereEqualTo("id", user?.uid)
                .get()
                .addOnSuccessListener { result ->
                    for (r in result) {
                        Log.d("TAG", "${r.id} => ${r.data.get("photoUrl")}")
//                        binding.imgProfile.setImageResource(r.data.get("photoUrl"))
                    }
                }
                .addOnFailureListener { exception ->
                    Log.w("TAG", "Error getting documents.", exception)
                }


        }
        binding.imgProfile.setOnClickListener {
            intentCamera()
        }
        binding.btnSimpan.setOnClickListener {

            val image = when{
                ::imageUri.isInitialized -> imageUri
                user?.photoUrl == null -> Uri.parse(R.drawable.icon_camera.toString())
                else -> user.photoUrl }

            val userDetail = hashMapOf(
                "id" to user?.uid,
                "name" to name,
                "jenisKelamin" to false,
                "noHp" to noHp,
                "photoUrl" to image,
                "alamat" to "",
                "poin" to ""
            )
            firebaseFirestore.collection(PROFILE_NAME)
                .whereEqualTo("id",user?.uid)
                .get()
                .addOnCompleteListener ( object : OnCompleteListener<QuerySnapshot> {
                    override fun onComplete(snapshot: Task<QuerySnapshot>) {
                        if (snapshot.isSuccessful && !snapshot.getResult().isEmpty){
                            val documentSnapshot = snapshot.getResult().documents.get(0)
                            val documentId = documentSnapshot.id
                            firebaseFirestore.collection(PROFILE_NAME)
                                .document(documentId)
                                .update(userDetail as Map<String, Any>)
                                .addOnSuccessListener (object : OnSuccessListener<Void>{
                                    override fun onSuccess(p0: Void?) {
                                        Toast.makeText(this@ProfileActivity,"Successfully Update Data",Toast.LENGTH_LONG).show()
                                    }
                                })
                                .addOnFailureListener(object : OnFailureListener {
                                    override fun onFailure(p0: Exception) {
                                        Toast.makeText(this@ProfileActivity,"Some Error for Update Data",Toast.LENGTH_LONG).show()
                                    }
                                })
                        }
                    }
                })
                .addOnFailureListener { e -> Log.w("ProfileActivity", "Error updating document", e) }

        }
    }



    @SuppressLint("QueryPermissionsNeeded")
    private fun intentCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
            packageManager?.let {
                intent.resolveActivity(it).also {
                    startActivityForResult(intent, REQUEST_CAMERA)
                }
            }
        }
    }


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CAMERA && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            uploadImage(imageBitmap)
        }
    }


    private fun uploadImage(imageBitmap: Bitmap) {
        val baos = ByteArrayOutputStream()
        val ref =
            FirebaseStorage.getInstance().reference.child("img/${FirebaseAuth.getInstance().currentUser?.uid}")

        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val image: ByteArray = baos.toByteArray()

        ref.putBytes(image)
            .addOnCompleteListener { upload ->
                if (upload.isSuccessful) {
                    ref.downloadUrl.addOnCompleteListener { task ->
                        task.result?.let { uri ->
                            imageUri = uri
                            binding.imgProfile.setImageBitmap(imageBitmap)
                        }
                    }
                }
            }
    }

    companion object {
        const val REQUEST_CAMERA = 100
        const val PROFILE = "profile"
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.btn_back ->{
                Intent(this,MainActivity::class.java).let {
                    startActivity(it)
                }
            }
        }
    }

}