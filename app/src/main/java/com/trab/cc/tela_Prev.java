package com.trab.cc;

import android.annotation.SuppressLint;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.Objects;

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
        } catch (MalformedURLException | ParseException e) {
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
    public File downloadData(String[] args) throws MalformedURLException, ParseException {
        Date date = new Date();
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
        dateFormat.format(date);
        LocalDate data_inicio = LocalDate.now().minusDays(10); //Data atual -10 dias.
        LocalDate data_fim = LocalDate.now(); //Data atual.
        DayOfWeek dia = DayOfWeek.of(data_fim.get(ChronoField.DAY_OF_WEEK));

        //Tratamento para a data_fim da request, visto que a API lança os valores somente em dias úteis e após as 12:59.
        if (dia.equals(DayOfWeek.SATURDAY)) {
                data_fim = LocalDate.now().minusDays(1); //Caso sábado, extrato de sexta.
        }else if(dia.equals(DayOfWeek.SUNDAY)){
                data_fim = LocalDate.now().minusDays(2); //Caso domingo, extrato de sexta.
        }else if (dia.equals(DayOfWeek.MONDAY)){
                data_fim = LocalDate.now().minusDays(3); //Caso segunda, extrato de sexta.
        }else if (Objects.requireNonNull(dateFormat.parse(dateFormat.format(date))).before(dateFormat.parse("14:01"))){
                data_fim = LocalDate.now().minusDays(1); //Caso antes de 12:59, extrato do dia anterior.
        }

        DateTimeFormatter data_format = DateTimeFormatter.ofPattern("dd-MM-YYYY"); //Formatação para adequar à request da API.
        String inicio = data_inicio.format(data_format); //String para compor a previsão_URL como parâmetro.
        String fim = data_fim.format(data_format); //String para compor a previsão_URL como parâmetro.
        String previsao_URL = "https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata/CotacaoDolarPeriodo(dataInicial=@dataInicial,dataFinalCotacao=@dataFinalCotacao)?@dataInicial='" + inicio + "'&@dataFinalCotacao='" + fim + "'&$top=100&$format=text/csv&$select=cotacaoCompra";
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
    public void previsao_Func() throws MalformedURLException, ParseException {
        ErrorCalculation.setMode(ErrorCalculationMode.RMS); //Seleciona o método de erro (rms = root mean square error)
        String[] args = new String[0]; //Define o arquivo com os dados de entrada.
        File filename = downloadData(args); //Especifica o nome do arquivo.
        CSVFormat format = new CSVFormat(',', ' '); //Especifica o separador de valores dentro do CSV.
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
        String saida = ""; //Processa a regressão.
        while (csv.next()) { //Lê as próximas linhas do arquivo csv.
            line[0] = csv.get(0); //Pega a cotação do dólar no registro do csv.
            helper.normalizeInputVector(line, slice, false); //Normaliza a entrada.
            MLData output = bestMethod.compute(input); //Calcula a regressão.
            dolarPrevisto = helper.denormalizeOutputVectorToString(output)[0]; //Captura a coluna 0 do vetor de retorno, que é o valor previsto.
        }
    }
}