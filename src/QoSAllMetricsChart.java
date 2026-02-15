import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.FileReader;

public class QoSAllMetricsChart {
    public static void main(String[] args) {
        String csvFile = "second.csv"; // change to your dataset

        // Create one series for each metric
        XYSeries mosSeries = new XYSeries("MOS");
        XYSeries bwSeries = new XYSeries("Bandwidth");
        XYSeries rttSeries = new XYSeries("RTT");
        XYSeries jitterSeries = new XYSeries("Jitter");
        XYSeries bufferSeries = new XYSeries("Buffer");
        XYSeries snrSeries = new XYSeries("SNR");

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            int time = 0;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");

                if (!values[0].equalsIgnoreCase("Time")) { // Skip header row
                    double mos = Double.parseDouble(values[1]);
                    double bw = Double.parseDouble(values[2]);
                    double rtt = Double.parseDouble(values[3]);
                    double jitter = Double.parseDouble(values[4]);
                    double buffer = Double.parseDouble(values[5]);
                    double snr = Double.parseDouble(values[6]);

                    mosSeries.add(time, mos);
                    bwSeries.add(time, bw);
                    rttSeries.add(time, rtt);
                    jitterSeries.add(time, jitter);
                    bufferSeries.add(time, buffer);
                    snrSeries.add(time, snr);

                    time++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Add all series into dataset
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(mosSeries);
        dataset.addSeries(bwSeries);
        dataset.addSeries(rttSeries);
        dataset.addSeries(jitterSeries);
        dataset.addSeries(bufferSeries);
        dataset.addSeries(snrSeries);

        // Create chart
        JFreeChart chart = ChartFactory.createXYLineChart(
                "QoS Metrics over Time",
                "Time (s)",
                "Values",
                dataset
        );

        // ✅ Set axis scaling like in the paper
        XYPlot plot = chart.getXYPlot();

        NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        xAxis.setRange(0, 500); // X-axis: 0–500
        xAxis.setTickUnit(new NumberTickUnit(50)); // Tick every 50

        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setRange(0, 400); // Y-axis: 0–400
        yAxis.setTickUnit(new NumberTickUnit(100)); // Tick every 100

        // Show chart
        JFrame frame = new JFrame("QoS Metrics Chart");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new ChartPanel(chart));
        frame.pack();
        frame.setVisible(true);
    }
}
