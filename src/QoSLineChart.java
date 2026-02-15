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

public class QoSLineChart {
    public static void main(String[] args) {
        String csvFile = "first.csv"; // change to your dataset
        XYSeries series = new XYSeries("MOS");

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            int time = 0;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (!values[0].equalsIgnoreCase("Time")) { // skip header
                    double mos = Double.parseDouble(values[1]); // assuming MOS in 2nd column
                    series.add(time, mos);
                    time++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        XYSeriesCollection dataset = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart(
                "MOS over Time",
                "Time (s)",
                "MOS",
                dataset
        );

        // ✅ Set axis ranges like in paper
        XYPlot plot = chart.getXYPlot();

        NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        xAxis.setRange(0, 500); // X-axis: 0–500
        xAxis.setTickUnit(new NumberTickUnit(50)); // tick spacing: 50

        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setRange(0, 400); // Y-axis: 0–400
        yAxis.setTickUnit(new NumberTickUnit(100)); // tick spacing: 100

        JFrame frame = new JFrame("Line Chart Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new ChartPanel(chart));
        frame.pack();
        frame.setVisible(true);
    }
}
