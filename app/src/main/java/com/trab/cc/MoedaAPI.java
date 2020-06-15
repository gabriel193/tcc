package com.trab.cc;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface MoedaAPI {
    @GET("json/USD-BRL") //Final da URL da API com o parâmetro de método GET do Retrofit.
    Call<List<Moeda>> getMoedas (); //Especifica que a lista de referência serão os objetos declarados na classe moeda.
}
