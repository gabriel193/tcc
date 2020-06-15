package com.trab.cc;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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
 * Subclasse responsável por realizar a conversão.
 * Esta classe é chamada na MainActivity.
 */

public class tela_Conv extends Fragment {
    TextView dolar_View; //TextView da conversão.
    EditText valor_Edit; //Edição do valor inserado pelo usuário para a conversão.

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_conv, container, false); //Criação da view para o fragmento do layout.
        Button conversao = (Button) v.findViewById(R.id.converter); //Especificação do botão no layout.
        conversao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                conversao();
            }
        }); //Tratamento para que quando o botão seja clicado, a função de conversão ser chamada.
        return v;
    }

    public void conversao() {
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
                    NumberFormat formatter = new DecimalFormat("#0.000"); //Formatação para os valores double.
                    dolar_View = getActivity().findViewById(R.id.dolar);
                    valor_Edit = getActivity().findViewById(R.id.edit_Real);
                    String digitado = valor_Edit.getText().toString();
                    if (!digitado.equals("")) { //Verifica se o valor digitado não está em branco, caso não esteja, realiza o cálculo.
                        dolar_View.setText(formatter.format(Double.parseDouble(valor_Edit.getText().toString()) / moeda.getBid()));
                    } else { //Caso esteja em branco, nada acontece.
                        return;
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Moeda>> call, Throwable t) {

            }
        });
    }
}