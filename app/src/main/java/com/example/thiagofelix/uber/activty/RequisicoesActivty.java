package com.example.thiagofelix.uber.activty;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.TextView;

import com.example.thiagofelix.uber.R;
import com.example.thiagofelix.uber.adapter.RequisicoesAdapter;
import com.example.thiagofelix.uber.config.ConfiguracaoFirebase;
import com.example.thiagofelix.uber.helper.RecyclerItemClickListener;
import com.example.thiagofelix.uber.helper.UsuarioFirebase;
import com.example.thiagofelix.uber.model.Requisicao;
import com.example.thiagofelix.uber.model.Usuario;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class RequisicoesActivty extends AppCompatActivity {
    private FirebaseAuth autenticacao;
    private DatabaseReference firebaseRef;
    private RecyclerView recyclerRequisicoes;
    private TextView textResultado;
    private List<Requisicao> listaRequisicoes = new ArrayList<>();
    private RequisicoesAdapter requisicoesAdapter;
    private Usuario motorista;

    private LocationManager locationManager;
    private LocationListener locationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_requisicoes_activty);
        inicializarComponentes();

        //recuperar localização
        recuperarLocalizacaoUsuario();

    }

    @Override
    protected void onStart() {
        super.onStart();
        verificaStatusRequisicao();
    }

    private void abrirTelaCorrida(String idRequisicao, Usuario motorista, boolean requisicaoAtiva){

        Intent i  = new Intent(RequisicoesActivty.this, CorridaActivty.class);
        i.putExtra("idRequisicao",idRequisicao);
        i.putExtra("motorista",motorista);
        i.putExtra("requisicaoAtiva",requisicaoAtiva);
        startActivity(i);

    }
    private void verificaStatusRequisicao(){
        Usuario usuarioLogado = UsuarioFirebase.getDadosUsuarioLogado();
        DatabaseReference firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();

        DatabaseReference requisicoes = firebaseRef.child("requisicoes");
        Query requisicoesPesquisas = requisicoes.orderByChild("motorista/id")
                .equalTo(usuarioLogado.getId());
        requisicoesPesquisas.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot ds: dataSnapshot.getChildren()){
                    Requisicao requisicao = ds.getValue(Requisicao.class);
                    if(requisicao.getStatus().equals(Requisicao.STATUS_A_CAMINHO)
                    || requisicao.getStatus().equals(Requisicao.STATUS_VIAGEM)
                    || requisicao.getStatus().equals(Requisicao.STATUS_FINALIZADA)){
                        motorista = requisicao.getMotorista();
                        abrirTelaCorrida(requisicao.getId(),motorista,true);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
    private void recuperarLocalizacaoUsuario() {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                //recuperar latitude / longitude
                String latitude = String.valueOf(location.getLatitude());
                String longitude = String.valueOf(location.getLongitude());
                //atualizar Geofire
                UsuarioFirebase.atualizarDadosLocaliacao(location.getLatitude(),location.getLongitude());

                if(!latitude.isEmpty() && !longitude.isEmpty()){
                    motorista.setLatitude(latitude);
                    motorista.setLongitude(longitude);

                    adcicionaEventoCliqueRecyclerView();
                    locationManager.removeUpdates(locationListener);
                    requisicoesAdapter.notifyDataSetChanged();


                }

            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };


        //solicitar atualizações do localização
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(
                    locationManager.GPS_PROVIDER,
                    0,
                    0,
                    locationListener

            );
            return;
        }

    }

    public void inicializarComponentes(){

        recyclerRequisicoes = findViewById(R.id.recyclerRequisicoes);
        textResultado = findViewById(R.id.textResultado);

        getSupportActionBar().setTitle("Requisições");
        //configurações inicias
        motorista = UsuarioFirebase.getDadosUsuarioLogado();
        autenticacao = ConfiguracaoFirebase.getFirabaseAutenticacao();
        firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();

      //configurar recyclerRequisicoes
        requisicoesAdapter = new RequisicoesAdapter(listaRequisicoes,getApplicationContext(), motorista);
      RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
      recyclerRequisicoes.setLayoutManager(layoutManager);
      recyclerRequisicoes.setHasFixedSize(true);
      recyclerRequisicoes.setAdapter(requisicoesAdapter);
        recuperarRequisicoes();
    }
    private  void adcicionaEventoCliqueRecyclerView(){

        //adiciona evento de clique ao recyclerRequisicoes
        recyclerRequisicoes.addOnItemTouchListener(
                new RecyclerItemClickListener(
                        getApplicationContext(),
                        recyclerRequisicoes,
                        new RecyclerItemClickListener.OnItemClickListener() {
                            @Override
                            public void onItemClick(View view, int position) {
                                Requisicao requisicao = listaRequisicoes.get(position);
                                abrirTelaCorrida(requisicao.getId(),motorista,false);
                            }

                            @Override
                            public void onLongItemClick(View view, int position) {

                            }

                            @Override
                            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                            }
                        }
                ) {
                }
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu2) {
        getMenuInflater().inflate(R.menu.menu_main,menu2);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){
            case R.id.menusair:
                autenticacao.signOut();
                finish();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void recuperarRequisicoes(){
        DatabaseReference requisicoes = firebaseRef.child("requisicoes");
        final Query requisicaoPesquisa = requisicoes.orderByChild("status")
                .equalTo(Requisicao.STATUS_AGUARDANDO);

        requisicaoPesquisa.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.getChildrenCount()>0){
                    textResultado.setVisibility(View.GONE);
                    recyclerRequisicoes.setVisibility(View.VISIBLE);
                }else{
                    textResultado.setVisibility(View.VISIBLE);
                    recyclerRequisicoes.setVisibility(View.GONE);
                }
                listaRequisicoes.clear();
                for (DataSnapshot ds: dataSnapshot.getChildren()){
                    Requisicao requisicao = ds.getValue(Requisicao.class);
                    listaRequisicoes.add(requisicao);
                }
                requisicoesAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }




}
