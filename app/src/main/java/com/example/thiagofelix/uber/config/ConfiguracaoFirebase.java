package com.example.thiagofelix.uber.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ConfiguracaoFirebase {
    public static FirebaseAuth autenticacao;
    public static DatabaseReference database;

    public static FirebaseAuth getFirabaseAutenticacao(){
        if(autenticacao == null){
            autenticacao = FirebaseAuth.getInstance();
        }
        return autenticacao;
    }
    public static DatabaseReference getFirebaseDatabase(){
        if(database == null){
            database = FirebaseDatabase.getInstance().getReference();
        }
        return database;
    }
}
