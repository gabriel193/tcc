package com.trab.cc;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Subclasse responsável por mostrar a cotação atual.
 * Esta classe é chamada na MainActivity.
 */

public class tela_Cota extends Fragment {
    NumberFormat formatter = new DecimalFormat("#0.000"); //Formatação para os valores double.
    TextView cotac_Compra; //Declaração da textview de compra.
    TextView cotac_Venda; //Declaração da textview de venda.

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cotacao_Func(); //Chamada da função quando a classe for iniciada.
    }

    public void cotacao_Func() {
        Retrofit retrofit = new Retrofit.Builder() //Cria o construtor do Retrofit.
                .baseUrl("https://economia.awesomeapi.com.br/") //Restante da url que foi declarada na interface MoedaAPI
                .addConverterFactory(GsonConverterFactory.create()) //O GsonFactory faz a leitura do jSON importado pela API.
                .build(); //Constrói o Json
        MoedaAPI MoedaAPI = retrofit.create(MoedaAPI.class);
        Call<List<Moeda>> call = MoedaAPI.getMoedas();
        call.enqueue(new Callback<List<Moeda>>() {
            @Override
            public void onResponse(Call<List<Moeda>> call, Response<List<Moeda>> response) {
                if (!response.isSuccessful()) { //Caso a resposta da API seja falha, a função retorná nada.
                    return;
                }
                List<Moeda> moedas = response.body(); //Atribuição da lista aos objetos adquiridos pelo corpo da requisição.
                for (Moeda moeda : moedas) {
                    cotac_Compra = getActivity().findViewById(R.id.cotacao_compra); //Especifica em qual activity minha view está.
                    cotac_Compra.setText(formatter.format(moeda.getBid())); //Seta o valor da view para o valor de compra.
                    cotac_Venda = getActivity().findViewById(R.id.cotacao_venda); //Especifica em qual activity minha view está.
                    cotac_Venda.setText(formatter.format(moeda.getAsk())); //Seta o valor da view para o valor de venda.
                }
            }

            @Override
            public void onFailure(Call<List<Moeda>> call, Throwable t) {
            }
        });
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cotac, container, false);
    }
}