package com.example.thiagofelix.uber.activty;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.thiagofelix.uber.R;
import com.example.thiagofelix.uber.config.ConfiguracaoFirebase;
import com.example.thiagofelix.uber.helper.UsuarioFirebase;
import com.example.thiagofelix.uber.model.Usuario;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

public class LoginActivity extends AppCompatActivity {
    private EditText campoEmail, campoSenha;
    private FirebaseAuth autenticacao;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        campoEmail= findViewById(R.id.editLoginEmail);
        campoSenha = findViewById(R.id.editLoginSenha);
    }
    public void verificarCamposLogin(View view){
        String email = campoEmail.getText().toString();
        String senha = campoSenha.getText().toString();

        if(!email.isEmpty()){

            if(!senha.isEmpty()){
                Usuario usuario = new Usuario();
                usuario.setEmail(email);
                usuario.setSenha(senha);
                logarUsuario(usuario);

            }else{
                Toast.makeText(getApplicationContext(),
                        "preencha a senha",
                        Toast.LENGTH_LONG).show();
            }

        }else{
            Toast.makeText(getApplicationContext(),
                    "preencha o email",
                    Toast.LENGTH_LONG).show();
        }

    }
    public void logarUsuario(Usuario usuario){
        autenticacao = ConfiguracaoFirebase.getFirabaseAutenticacao();
        autenticacao.signInWithEmailAndPassword(usuario.getEmail(),
                usuario.getSenha()
        ).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()){
                    //verificar o tipo de usuario e direcioná-lo para sua activty
                    UsuarioFirebase.redirecionaUsuarioLogado(LoginActivity.this);
                }else{
                    String excecao = "";
                    try {
                        throw task.getException();
                    }catch (FirebaseAuthInvalidUserException e) {
                        excecao = "Usuário não cadastrado";
                    }catch (FirebaseAuthInvalidCredentialsException e ){
                        excecao = "E-mail e senha não corresponde a um usuário cadastrado";
                    }catch (Exception e ){
                        excecao = "Erro ao cadastrar usuário"+ e.getMessage();
                        e.printStackTrace();
                    }


                    Toast.makeText(getApplicationContext(),
                            excecao,Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
