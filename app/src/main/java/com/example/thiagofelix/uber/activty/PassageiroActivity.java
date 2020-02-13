package com.example.thiagofelix.uber.activty;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;

import com.example.thiagofelix.uber.config.ConfiguracaoFirebase;
import com.example.thiagofelix.uber.helper.Local;
import com.example.thiagofelix.uber.helper.UsuarioFirebase;
import com.example.thiagofelix.uber.model.Destino;
import com.example.thiagofelix.uber.model.Requisicao;
import com.example.thiagofelix.uber.model.Usuario;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.thiagofelix.uber.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class PassageiroActivity extends AppCompatActivity
        implements OnMapReadyCallback {



    private GoogleMap mMap;
    private FirebaseAuth autenticacao;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private EditText editDestino;
    private  LatLng localPassageiro;
    private LinearLayout linearLayoutDestino;
    private Button buttonChamarUber;
    private boolean cancelarUber = false;
    private DatabaseReference firebaseRef;
    private Requisicao requisicao;
    private Marker marcadorPassageiro;
    private Usuario passageiro;
    private String statusRequisicao;
    private Destino destino;
    private Marker marcadorMotorista;
    private Marker marcadorDestino;
    private  Usuario motorista;
    private LatLng localMotorista;
    private String  resultado;

    /*
    * lat/long destino: -3.079139, -60.050966
    * lat/long passageiro: -3.080087, -60.053157
    *lat/long motorista(a caminho):
    * inicial: -3.080515, -60.053608
    * intermediário: -3.080088, -60.053608
    * final : -3.080087, -60.053106
    *-3.087450, -60.041130
    * -3.081719, -60.053853
    * */


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passageiro);



        inicializarComponentes();
        verificaStatusRequisicao();


    }
    private void verificaStatusRequisicao() {
        Usuario usuarioLodago = UsuarioFirebase.getDadosUsuarioLogado();
        DatabaseReference requisicoes = firebaseRef.child("requisicoes");
        final Query requisicaoPesquisa = requisicoes.orderByChild("passageiro/id")
                .equalTo(usuarioLodago.getId());

        requisicaoPesquisa.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                List<Requisicao> lista = new ArrayList<>();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    lista.add(ds.getValue(Requisicao.class));
                }
                Collections.reverse(lista);

                if(lista!=null && lista.size()>0){
                    requisicao = lista.get(0);

                    if(requisicao != null){
                        if(!requisicao.getStatus().equals(Requisicao.STATUS_ENCERRADA)) {
                            passageiro = requisicao.getPassageiro();
                            localPassageiro = new LatLng(
                                    Double.parseDouble(passageiro.getLatitude()),
                                    Double.parseDouble(passageiro.getLongitude())
                            );
                            statusRequisicao = requisicao.getStatus();
                            destino = requisicao.getDestino();
                            if (requisicao.getMotorista() != null) {
                                motorista = requisicao.getMotorista();
                                localMotorista = new LatLng(
                                        Double.parseDouble(motorista.getLatitude()),
                                        Double.parseDouble(motorista.getLongitude())
                                );

                            }
                            alteraInterfaceStatusRequisicao(statusRequisicao);
                        }
                    }


                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
    private void alteraInterfaceStatusRequisicao(String status){
        if(status !=null && !status.isEmpty()) {
            cancelarUber = false;
            switch (status) {
                case Requisicao.STATUS_AGUARDANDO:
                    requisicaoAguardando();

                    break;
                case Requisicao.STATUS_A_CAMINHO:
                    requisicaoACaminho();
                    break;
                case Requisicao.STATUS_VIAGEM:
                    requisicaoViagem();
                    break;
                case Requisicao.STATUS_FINALIZADA:
                    requisicaoFinalizada();
                    break;
                case Requisicao.STATUS_CANCELADA:
                    requisicaoCancelada();
                    break;
            }
        }else{
            //adiciona marcador passageiro
            adicionarMarcadorPassageiro(localPassageiro,"Meu local");
            centralizarMarcador(localPassageiro);

        }

    }
    private void requisicaoCancelada(){
        linearLayoutDestino.setVisibility(View.VISIBLE);
        buttonChamarUber.setText("Chamar Uber");
        cancelarUber = false;


    }
    private void requisicaoAguardando(){
        linearLayoutDestino.setVisibility(View.GONE);
        buttonChamarUber.setText("cancelar uber");
        cancelarUber = true;
        //adiciona marcador do passageiro
        adicionarMarcadorPassageiro(localPassageiro,passageiro.getNome());
        centralizarMarcador(localPassageiro);
    }
    private void requisicaoACaminho(){
        linearLayoutDestino.setVisibility(View.GONE);
        buttonChamarUber.setText("Motorista a camino");
        buttonChamarUber.setEnabled(false);
        //adiciona marcador passageiro
        adicionarMarcadorPassageiro(localPassageiro,passageiro.getNome());
        //adiciona marcador motorista
        adicionarMarcadorMotorista(localMotorista,motorista.getNome());
        //centralizar dois marcadores
        centralizarDoisMarcadores(marcadorMotorista,marcadorPassageiro);

    }
    private void requisicaoViagem(){
        linearLayoutDestino.setVisibility(View.GONE);
        buttonChamarUber.setText("A caminho do destino");
        buttonChamarUber.setEnabled(false);
        //adiciona marcador do motorista
        adicionarMarcadorMotorista(localMotorista,motorista.getNome());

       //adiciona marcador de destino
        LatLng localDestino =new LatLng(
                Double.parseDouble(destino.getLatitude()),
                Double.parseDouble(destino.getLongitude())
        );
        adicionarMarcadorDestino(localDestino,"Destino");
        //centralizar motorista e destino
        centralizarDoisMarcadores(marcadorMotorista,marcadorDestino);

    }
    private void requisicaoFinalizada(){
        linearLayoutDestino.setVisibility(View.GONE);
        buttonChamarUber.setText("Corrida finalizada");
        buttonChamarUber.setEnabled(false);
        //adiciona marcador de destino
        LatLng localDestino =new LatLng(
                Double.parseDouble(destino.getLatitude()),
                Double.parseDouble(destino.getLongitude())
        );
        adicionarMarcadorDestino(localDestino,"Destino");
        //calcular distancia
        float distancia = Local.calcularDistancia(localPassageiro,localDestino);
        float valorMinimo = 5;
        if(distancia < 2){
            resultado = String.valueOf(valorMinimo);
        }else {
            float valorkm = valorMinimo + (distancia-2)*3;
            DecimalFormat decimal = new DecimalFormat("0.00");
            resultado = decimal.format(valorkm);
        }
        buttonChamarUber.setText("Corrida finalizada - R$ "+resultado);
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("total viagem")
                .setMessage("Pague R$"+ resultado + " ao seu motorista")
                .setCancelable(false)
                .setNegativeButton("Encerrar viagem", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        requisicao.setStatus(Requisicao.STATUS_ENCERRADA);
                        requisicao.atualizarStatus();

                        finish();
                        startActivity(new Intent(getIntent()));
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();


    }
    private void adicionarMarcadorPassageiro(LatLng localizacao, String titulo){
        if(marcadorPassageiro != null)
            marcadorPassageiro.remove();

        marcadorPassageiro = mMap.addMarker(
                new MarkerOptions()
                        .position(localizacao)
                        .title(titulo)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.usuario))

        );
    }
    private void centralizarMarcador(LatLng local){
        mMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(local,20)
        );
    }
    private void adicionarMarcadorMotorista(LatLng localizacao, String titulo){
        if(marcadorMotorista != null)
            marcadorMotorista.remove();

        marcadorMotorista = mMap.addMarker(
                new MarkerOptions()
                        .position(localizacao)
                        .title(titulo)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.carro))

        );

    }
    private void adicionarMarcadorDestino(LatLng localizacao, String titulo){

        if(marcadorPassageiro != null)
            marcadorPassageiro.remove();

        if(marcadorDestino != null)
            marcadorDestino.remove();

        marcadorDestino = mMap.addMarker(
                new MarkerOptions()
                        .position(localizacao)
                        .title(titulo)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.destino))

        );

    }
    private void centralizarDoisMarcadores(Marker marcador1, Marker marcador2){
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(marcador1.getPosition());
        builder.include(marcador2.getPosition());

        LatLngBounds bounds = builder.build();
        int largura = getResources().getDisplayMetrics().widthPixels;
        int altura  = getResources().getDisplayMetrics().heightPixels;
        int espacoInterno = (int) (largura*0.2);
        mMap.moveCamera(
                CameraUpdateFactory.newLatLngBounds(bounds,largura,altura,espacoInterno)
        );
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //recuperar localização
        recuperarLocalizacaoUsuario();


    }
    private  void salvarRequisicao(Destino destino ){
        Requisicao requisicao = new Requisicao();
        requisicao.setDestino(destino);

        Usuario usuarioPassageiro = UsuarioFirebase.getDadosUsuarioLogado();
        usuarioPassageiro.setLatitude(String.valueOf(localPassageiro.latitude));
        usuarioPassageiro.setLongitude(String.valueOf(localPassageiro.longitude));


        requisicao.setPassageiro(usuarioPassageiro);
        requisicao.setStatus(Requisicao.STATUS_AGUARDANDO);
        requisicao.salvar();

        linearLayoutDestino.setVisibility(View.GONE);
        buttonChamarUber.setText("cancelar uber");

    }

    public void chamarUber(View view){

        //false > uber não pode ser cancelado ainda
        //true > uber pode ser cancelado
        if(cancelarUber){//uber não pode ser cancelado
            //cancelar requisição
            requisicao.setStatus(Requisicao.STATUS_CANCELADA);
            requisicao.atualizarStatus();


        }else{
            String enderecoDestino = editDestino.getText().toString();
            if(!enderecoDestino.equals("")|| enderecoDestino != null){
                Address addressDestino = recuperarEndereco(enderecoDestino);
                if(addressDestino != null){
                    final Destino destino = new Destino();
                    destino.setCidade(addressDestino.getAdminArea());
                    destino.setCep(addressDestino.getPostalCode());
                    destino.setBairro(addressDestino.getSubLocality());
                    destino.setRua(addressDestino.getThoroughfare());
                    destino.setNumero(addressDestino.getFeatureName());
                    destino.setLatitude(String.valueOf(addressDestino.getLatitude()));
                    destino.setLongitude(String.valueOf(addressDestino.getLongitude()));

                    StringBuilder mensagem = new StringBuilder();
                    mensagem.append("Cidade: " + destino.getCidade());
                    mensagem.append(" \n Bairro: " + destino.getBairro());
                    mensagem.append(" \n Rua: " + destino.getRua());
                    mensagem.append(" \n Numero: " + destino.getNumero());
                    mensagem.append(" \n Cep: " + destino.getCep());

                    AlertDialog.Builder builder = new AlertDialog.Builder(this)
                            .setTitle("Confirme o endereço de destino")
                            .setMessage(mensagem)
                            .setPositiveButton("Confirmar", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                    //salvar a requisição
                                    salvarRequisicao(destino);

                                }
                            }).setNegativeButton("cancelar", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                }
                            });
                    AlertDialog dialog = builder.create();
                    dialog.show();


                }

            }else{
                Toast.makeText(getApplicationContext(),"Digite o endereço de destino",Toast.LENGTH_LONG).show();
            }

        }



    }

    public Address recuperarEndereco(String endereco){
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> listaEnderecos = geocoder.getFromLocationName(endereco,1);
            if(listaEnderecos != null && listaEnderecos.size() >0){
                Address address = listaEnderecos.get(0);

                double latitude = address.getLatitude();
                double longitude = address.getLongitude();

                return address;

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void recuperarLocalizacaoUsuario() {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                //recuperar latitude / longitude
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                localPassageiro = new LatLng(latitude, longitude);

                //atualizar geofire
                UsuarioFirebase.atualizarDadosLocaliacao(latitude,longitude);

               //altera interface de acordo com o status
                alteraInterfaceStatusRequisicao(statusRequisicao);
                if(statusRequisicao !=null && !statusRequisicao.isEmpty()) {
                    if (statusRequisicao.equals(Requisicao.STATUS_VIAGEM)
                            || statusRequisicao.equals(Requisicao.STATUS_FINALIZADA)) {
                        locationManager.removeUpdates(locationListener);

                    }else{    //solicitar atualizações do localização
                        if (ActivityCompat.checkSelfPermission(PassageiroActivity.this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            locationManager.requestLocationUpdates(
                                    locationManager.GPS_PROVIDER,
                                    0,
                                    5,
                                    locationListener
                            );
                        }
                    }
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
        if (ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(
                    locationManager.GPS_PROVIDER,
                    0,
                    5,
                    locationListener

            );
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
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

    private void inicializarComponentes(){
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Iniciar uma viagem");
        setSupportActionBar(toolbar);

        //configurações inicias
        autenticacao = ConfiguracaoFirebase.getFirabaseAutenticacao();
        firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();

        //inicializar compontentes
        editDestino = findViewById(R.id.editDestino);
        // confirar mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        buttonChamarUber = findViewById(R.id.buttonChamarUber);
        linearLayoutDestino = findViewById(R.id.linearLayoutDestino);

    }

}
