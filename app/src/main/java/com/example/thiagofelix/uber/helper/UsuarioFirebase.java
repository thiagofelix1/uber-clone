package com.example.thiagofelix.uber.helper;

import android.app.Activity;
import android.content.Intent;
import android.location.LocationListener;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.thiagofelix.uber.activty.PassageiroActivity;
import com.example.thiagofelix.uber.activty.RequisicoesActivty;
import com.example.thiagofelix.uber.config.ConfiguracaoFirebase;
import com.example.thiagofelix.uber.model.Usuario;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class UsuarioFirebase {

    public static final Usuario getDadosUsuarioLogado (){
        FirebaseUser firebaseUser = getUsuarioAtual();
        Usuario usuario = new Usuario();
        usuario.setId(firebaseUser.getUid());
        usuario.setEmail(firebaseUser.getEmail());
        usuario.setNome(firebaseUser.getDisplayName());

        return usuario;
    }

    public static FirebaseUser getUsuarioAtual(){
        FirebaseAuth usuario = ConfiguracaoFirebase.getFirabaseAutenticacao();
       return usuario.getCurrentUser();
    }

    public static boolean atualizarNomeUsuario(String nome){
       try {
           FirebaseUser user = getUsuarioAtual();
           UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                   .setDisplayName(nome).build();

           user.updateProfile(profile).addOnCompleteListener(new OnCompleteListener<Void>() {
               @Override
               public void onComplete(@NonNull Task<Void> task) {
                   Log.d("perfil: ","erro ao att o nome de perfil");

               }
           });
           return true;
       }catch (Exception e ){
           e.printStackTrace();
           return false;
       }
    }
    public static void redirecionaUsuarioLogado(final Activity activity){

        FirebaseUser user = getUsuarioAtual();
        if(user != null){
            DatabaseReference usuariosRef = ConfiguracaoFirebase.getFirebaseDatabase()
                    .child("usuarios")
                    .child(getIdentificadorUsuario());
            usuariosRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Usuario usuario = dataSnapshot.getValue(Usuario.class);
                    String tipoUsuario = usuario.getTipo();

                    if(tipoUsuario.equals("M")){
                        activity.startActivity(new Intent(activity, RequisicoesActivty.class));
                    }else{
                        activity.startActivity(new Intent(activity, PassageiroActivity.class));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }


    }
    public static void atualizarDadosLocaliacao(double lat , double lon){
        //define local usuario
        DatabaseReference localUsuario = ConfiguracaoFirebase.getFirebaseDatabase()
                .child("local_usuario");
        GeoFire geoFire = new GeoFire(localUsuario);
        //recupera dados do usuário logado
        Usuario usuarioLogado = UsuarioFirebase.getDadosUsuarioLogado();

        //configura localização do usuário
        geoFire.setLocation(
                usuarioLogado.getId(),
                new GeoLocation(lat, lon),
                new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        if(error != null){
                            Log.d("Erro", "erro ao salvar o local do usuario");
                        }else{
                            Log.d("Geofire", "sucesso ao salvar o local do usuario");

                        }

                    }
                }
        );


    }

    public static String getIdentificadorUsuario(){
        return getUsuarioAtual().getUid();
    }


}
