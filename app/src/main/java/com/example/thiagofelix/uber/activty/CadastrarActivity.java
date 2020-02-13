package com.example.thiagofelix.uber.activty;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
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
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

public class CadastrarActivity extends AppCompatActivity {
    private EditText campoNome,campoEmail,campoSenha;
    private Switch switchUsuario;
    private FirebaseAuth autenticacao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastrar);
        campoNome = findViewById(R.id.editNome);
        campoEmail = findViewById(R.id.editEmail);
        campoSenha = findViewById(R.id.editSenha);
        switchUsuario = findViewById(R.id.switchUsuario);
    }

    public void validarCadastroUsuario(View view){
        //recuperar os textos de dados do usuario
        String nome = campoNome.getText().toString();
        String email = campoEmail.getText().toString();
        String senha = campoSenha.getText().toString();

        if (!nome.isEmpty()){

            if(!email.isEmpty()){

                if(!senha.isEmpty()){
                    Usuario usuario = new Usuario();
                    usuario.setNome(nome);
                    usuario.setEmail(email);
                    usuario.setSenha(senha);
                    usuario.setTipo( verificaTipoUsuario() );
                    cadastrarUsuario(usuario);


                }else{
                    Toast.makeText(getApplicationContext(),"preencha a senha",Toast.LENGTH_LONG).show();

                }

            }else{
                Toast.makeText(getApplicationContext(),"preencha o email",Toast.LENGTH_LONG).show();

            }
        }else {
            Toast.makeText(getApplicationContext(),"preencha o nome",Toast.LENGTH_LONG).show();
        }

    }

    public void cadastrarUsuario(final Usuario usuario){

        autenticacao = ConfiguracaoFirebase.getFirabaseAutenticacao();
        autenticacao.createUserWithEmailAndPassword(
                usuario.getEmail(),
                usuario.getSenha()
        ).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {

                if(task.isSuccessful()){
                    try {
                        String idUsuario = task.getResult().getUser().getUid();
                        usuario.setId(idUsuario);
                        usuario.salvar();

                        //Atualizar nome no userProfile para recuperar innformações que vamos utilizar frequentemente de maneira mais rápida
                        UsuarioFirebase.atualizarNomeUsuario(usuario.getNome());


                        if(verificaTipoUsuario() == "P"){
                            startActivity(new Intent( CadastrarActivity.this, PassageiroActivity.class ));
                            finish();
                            Toast.makeText(getApplicationContext(),
                                    "sucesso ao cadastrar passegeiro",
                                    Toast.LENGTH_LONG).show();
                        }else{
                            startActivity(new Intent( CadastrarActivity.this, RequisicoesActivty.class ));
                            finish();
                            Toast.makeText(getApplicationContext(),
                                    "sucesso ao cadastrar motorista",
                                    Toast.LENGTH_LONG).show();
                        }
                    }catch (Exception e ){
                        e.printStackTrace();
                    }



                }else {
                    String excecao = "";
                    try {
                        throw task.getException();
                    }catch (FirebaseAuthWeakPasswordException e ){
                        excecao = "digite uma senha mais forte";
                    }catch (FirebaseAuthInvalidCredentialsException e ){
                        excecao = "digite um email válido";
                    }catch (FirebaseAuthUserCollisionException e ) {
                        excecao = "email já cadastrado";
                    }catch (Exception e ){
                        excecao = "erro ao cadastrar usuário"+ e.getMessage();
                        e.printStackTrace();
                    }


                    Toast.makeText(getApplicationContext(),excecao,Toast.LENGTH_LONG).show();
                }

            }
        });
    }

    public String verificaTipoUsuario (){
        return switchUsuario.isChecked() ? "M" : "P" ;

    }



}
