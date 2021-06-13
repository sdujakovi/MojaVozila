package com.example.mojavozila

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class LogInActivity : Activity() {

    lateinit var editTextIme: EditText
    lateinit var editTextPrezime: EditText
    lateinit var  buttonKreni: Button
    lateinit var prijavljeniKorisnik: Korisnik
    lateinit var ref: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_in)

        ref = FirebaseDatabase.getInstance().getReference("korisnici")

        editTextIme = findViewById(R.id.ime_id)
        editTextPrezime = findViewById(R.id.prezime_id)
        buttonKreni = findViewById(R.id.buttonKreni)

            buttonKreni.setOnClickListener{
                pokreniPracenje()
                val intent = Intent(this, MapsActivity::class.java)
                intent.putExtra("id", prijavljeniKorisnik.id)
                editTextIme.text.clear()
                editTextPrezime.text.clear()
                startActivity(intent)

        }
    }

    private fun pokreniPracenje(){
        val ime = editTextIme.text.toString().capitalize()
        val prezime = editTextPrezime.text.toString().capitalize()

        if(ime.isEmpty()){
            editTextIme.error = "Unesi ime!"
            return
        }
        val korisnikId = ref.push().key!!

        prijavljeniKorisnik = Korisnik(korisnikId, ime, prezime, 0.0, 0.0)
        ref.child(korisnikId).setValue(prijavljeniKorisnik).addOnCompleteListener{
        }
    }
}