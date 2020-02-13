package com.example.thiagofelix.uber.activty;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import com.example.thiagofelix.uber.R;
import com.example.thiagofelix.uber.config.ConfiguracaoFirebase;
import com.example.thiagofelix.uber.helper.Permissoes;
import com.example.thiagofelix.uber.helper.UsuarioFirebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth autenticacao;
    private String[] permissoes = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().hide();
        //VALIDAR PERMISSÕES

        /*autenticacao = ConfiguracaoFirebase.getFirabaseAutenticacao();
        autenticacao.signOut();*/

        Permissoes.validarPermissoes(permissoes,this,1);

    }
    public void entrar(View view){
        startActivity(new Intent(this,LoginActivity.class));
    }
    public void cadastrar(View view){
        startActivity(new Intent(this,CadastrarActivity.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        UsuarioFirebase.redirecionaUsuarioLogado(MainActivity.this);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for(int permisaoResultado : grantResults){
            if(permisaoResultado == PackageManager.PERMISSION_DENIED){
                alertaValidacaoPermissao();

            }
        }
    }
    public void alertaValidacaoPermissao(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permissões negadas");
        builder.setMessage("permitir que o app utilize sua localização");
        builder.setCancelable(false);
        builder.setPositiveButton("Confirmar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();

    }
}
