package com.trab.cc;

import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import org.encog.bot.BotUtil;
import org.encog.mathutil.error.ErrorCalculation;
import org.encog.mathutil.error.ErrorCalculationMode;
import org.encog.ml.MLRegression;
import org.encog.ml.data.MLData;
import org.encog.ml.data.versatile.NormalizationHelper;
import org.encog.ml.data.versatile.VersatileMLDataSet;
import org.encog.ml.data.versatile.columns.ColumnDefinition;
import org.encog.ml.data.versatile.columns.ColumnType;
import org.encog.ml.data.versatile.sources.CSVDataSource;
import org.encog.ml.data.versatile.sources.VersatileDataSource;
import org.encog.ml.factory.MLMethodFactory;
import org.encog.ml.model.EncogModel;
import org.encog.util.csv.CSVFormat;
import org.encog.util.csv.ReadCSV;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Subclasse responsável por calcular a previsão.
 * Esta classe é chamada na MainActivity.
 */

public class tela_Prev extends Fragment {
    String local_Temp;
    String dolarPrevisto;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build(); //Permite que as webrequests sejam feitas de forma síncrona.
        StrictMode.setThreadPolicy(policy); //Permite que as webrequests sejam feitas de forma síncrona.
        try { //Pequeno try catch para que caso a URL seja inválida, o app não dê uma exceção.
            previsao_Func();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        NumberFormat formatter = new DecimalFormat("#0.000"); //Formatação para os valores double.
        View v = inflater.inflate(R.layout.fragment_prev, container, false);
        TextView previ = (TextView) v.findViewById(R.id.previsao);
        previ.setText(formatter.format(Double.parseDouble(dolarPrevisto)));
        return v;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public File downloadData(String[] args) throws IOException {
        LocalDate data_fim = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-YYYY"); //Formatação para adequar à request da API.
        String data_final = formatter.format(data_fim);
        String previsao_URL = "https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata/CotacaoDolarPeriodo(dataInicial=@dataInicial,dataFinalCotacao=@dataFinalCotacao)?@dataInicial='01-01-2020'&@dataFinalCotacao='" + data_final + "'&$top=30&$orderby=dataHoraCotacao%20desc&$format=text/csv&$select=cotacaoCompra";
        if (args.length != 0) {
            local_Temp = args[0];
        } else {
            local_Temp = System.getProperty("java.io.tmpdir"); //Diretório temporário para armazenamento do arquivo baixado.
        }
        File filename = new File(local_Temp, "auto-mpg.data");
        BotUtil.downloadPage(new URL(previsao_URL), filename);
        return filename;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void previsao_Func() throws IOException {
        ErrorCalculation.setMode(ErrorCalculationMode.RMS); //Seleciona o método de erro (rms = root mean square error)
        String[] args = new String[0]; //Define o arquivo com os dados de entrada.
        File filename = downloadData(args); //Especifica o nome do arquivo.
        CSVFormat format = new CSVFormat(',', ' ');  //Especifica o separador de valores dentro do CSV.
        VersatileDataSource source = new CSVDataSource(filename, true, format); //Mapeia o arquivo de entrada em um "VersatileDatasource"
        VersatileMLDataSet data = new VersatileMLDataSet(source);
        data.getNormHelper().setFormat(format); //Inicializa o ajudante de normalização.
        ColumnDefinition columnDolar = data.defineSourceColumn("cotacaoCompra", ColumnType.continuous); //Define o formato do arquivo e especifica a coluna.
        data.analyze(); //Analisa o arquivo.
        data.defineInput(columnDolar); //Especifica que a coluna de entrada será a columnDolar
        data.defineOutput(columnDolar); //Especifica que a coluna de saída será a columnDolar
        EncogModel model = new EncogModel(data); //Cria rede neural
        model.selectMethod(data, MLMethodFactory.TYPE_FEEDFORWARD); //Especifica o tipo da rede que é FEEDFORWARD
        data.normalize(); //Normaliza os dados, a configuração está como automática a partir do modelo escolhido.
        data.setLeadWindowSize(1);//Especificações da série de dados.
        data.setLagWindowSize(3);//Especificações da série de dados.
        model.holdBackValidation(0.3, false, 1001); //Define os dados para validação final (30% da base, sem embaralhar, semente fixa).
        model.selectTrainingType(data);//Seleciona o tipo de treinamento de acordo com o modelo.

        //Faz o treinamento dos dados com validação cruzada de 3 dobras.
        //Retorna o melhor método encontrado, no caso será BasicNetwork
        MLRegression bestMethod = (MLRegression) model.crossvalidate(3, false);
        NormalizationHelper helper = data.getNormHelper(); //Inicia os parâmetros de normalização.
        ReadCSV csv = new ReadCSV(filename, true, format); //Formata e lê como csv.
        String[] line = new String[1]; //  Vetor de entrada (Somente a cotação)
        double[] slice = new double[1]; // Vetor de saída (Somente UM double)
        MLData input = helper.allocateInputVector(5); //Pegas as últimas 5 cotações do arquivo.
        while (csv.next()) { //Lê as próximas linhas do arquivo csv.
            line[0] = csv.get(0); //Pega a cotação do dólar no registro do csv.
            helper.normalizeInputVector(line, slice, false); //Normaliza a entrada.
            MLData output = bestMethod.compute(input); //Calcula a regressão.
            dolarPrevisto = helper.denormalizeOutputVectorToString(output)[0]; //Captura a coluna 0 do vetor de retorno, que é o valor previsto.
        }
    }
}