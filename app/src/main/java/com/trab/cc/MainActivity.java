package com.trab.cc;

import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.ismaeldivita.chipnavigation.ChipNavigationBar;

@RequiresApi(api = Build.VERSION_CODES.O)
public class MainActivity extends AppCompatActivity {
    ChipNavigationBar bottomNav; //Definição da navegação dos botões na tela inicial.
    FragmentManager fragmentManager; //Criação dos fragmentos (páginas) de layout.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); //Define o layout principal como activity_main.xml
        bottomNav = findViewById(R.id.bottom_nav); //Referencia a navegação dos botões.

        if (savedInstanceState == null) { //Essa checagem basicamente faz com que o app sempre inicie na tela inicial.
            bottomNav.setItemSelected(R.id.home, true);
            fragmentManager = getSupportFragmentManager();
            tela_Cota telaCota = new tela_Cota();
            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, telaCota) //Substitui o fragmento da visualização pela tela de cotação.
                    .commit(); //Salva.
        }

        bottomNav.setOnItemSelectedListener(new ChipNavigationBar.OnItemSelectedListener() { //Cria o listener de cliques nos botões.
            @Override
            public void onItemSelected(int id) { //Verifica se o item está selecionado.
                Fragment fragment = null;
                switch (id) { //A partir deste switch, o item selecionado será a página que será trocada.
                    case R.id.home:
                        fragment = new tela_Cota(); //Página com a cotação.
                        break;
                    case R.id.conversao:
                        fragment = new tela_Conv(); //Página com a conversão.
                        break;
                    case R.id.prev:
                        fragment = new tela_Prev(); //Página com a previsão
                        break;
                }

                if (fragment != null) { //Caso algum botão seja apertado, pega o valor setado para o fragment e seta como a tela.
                    fragmentManager = getSupportFragmentManager();
                    fragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, fragment)
                            .commit();
                }
            }
        });
    }

}
