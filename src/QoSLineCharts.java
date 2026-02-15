import com.opencsv.CSVReader;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import javax.swing.*;
import java.io.FileReader;
import java.util.*;

public class QoSLineCharts {
    public static void main(String[] args) throws Exception {
        String csvFile = "first.csv"; // replace with Mario sir’s dataset
        CSVReader reader = new CSVReader(new FileReader(csvFile));

        List<Double> mos = new ArrayList<>();
        List<Double> bandwidth = new ArrayList<>();
        List<Double> rtt = new ArrayList<>();
        List<Double> jitter = new ArrayList<>();
        List<Double> buffer = new ArrayList<>();
        List<Double> snr = new ArrayList<>();

        String[] nextLine;
        reader.readNext(); // skip header
        while ((nextLine = reader.readNext()) != null) {
            mos.add(Double.parseDouble(nextLine[0]));
            bandwidth.add(Double.parseDouble(nextLine[1]));
            rtt.add(Double.parseDouble(nextLine[2]));
            jitter.add(Double.parseDouble(nextLine[3]));
            buffer.add(Double.parseDouble(nextLine[4]));
            snr.add(Double.parseDouble(nextLine[5]));
        }
        reader.close();

        // Create charts for all 6 variables
        createChart(mos, "MOS over Time", "Time", "MOS");
        createChart(bandwidth, "Bandwidth over Time", "Time", "kb/s");
        createChart(rtt, "RTT over Time", "Time", "ms");
        createChart(jitter, "Jitter over Time", "Time", "ms");
        createChart(buffer, "Buffer over Time", "Time", "ms");
        createChart(snr, "SNR over Time", "Time", "dB");
    }

    private static void createChart(List<Double> data, String title, String xLabel, String yLabel) {
        XYSeries series = new XYSeries(title);
        for (int i = 0; i < data.size(); i++) {
            series.add(i, data.get(i));
        }
        XYSeriesCollection dataset = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart(title, xLabel, yLabel, dataset);

        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new ChartPanel(chart));
        frame.pack();
        frame.setVisible(true);
    }
}
