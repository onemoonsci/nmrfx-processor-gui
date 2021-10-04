/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nmrfx.processor.gui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToolBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.controlsfx.control.PropertySheet;
import org.controlsfx.control.PropertySheet.Item;
import org.nmrfx.datasets.DatasetBase;
import org.nmrfx.processor.datasets.Dataset;
import org.nmrfx.datasets.DatasetRegion;
import org.nmrfx.processor.datasets.Measure;
import org.nmrfx.processor.datasets.Measure.MeasureTypes;
import org.nmrfx.processor.datasets.Measure.OffsetTypes;
import static org.nmrfx.processor.gui.PreferencesController.getDatasetDirectory;
import org.nmrfx.processor.gui.controls.FileTableItem;
import org.nmrfx.processor.gui.controls.ScanTable;
import org.nmrfx.processor.gui.tools.TRACTGUI;
import org.nmrfx.utils.GUIUtils;
import org.nmrfx.utils.properties.DirectoryOperationItem;
import org.nmrfx.utils.properties.TextOperationItem;

/**
 * FXML Controller class
 *
 * @author Bruce Johnson
 */
public class ScannerController implements Initializable {

    @FXML
    ToolBar scannerBar;
    @FXML
    private VBox mainBox;
    @FXML
    private VBox opBox;
    @FXML
    private Button scanDirChooserButton;
    @FXML
    private Button loadTableChooserButton;
    @FXML
    private Button processScanDirButton;
    @FXML
    private Button measureButton;
    @FXML
    private CheckBox combineFiles;
    @FXML
    private TableView<FileTableItem> tableView;
    @FXML
    private PropertySheet parSheet;
    @FXML
    private TabPane tabPane;

    FXMLController fxmlController;
    PolyChart chart;
    Stage stage;
    ScanTable scanTable;
    ChoicePropertyItem measureItem;
    OffsetPropertyItem offsetItem;
    DirectoryOperationItem scanDirItem;
    DirectoryOperationItem outputDirItem;
    TextOperationItem outputFileItem;
    ChangeListener<String> scanDirListener;
    ChangeListener<String> outputDirListener;
    static Consumer createControllerAction = null;
    TRACTGUI tractGUI = null;

    static final Pattern WPAT = Pattern.compile("([^:]+):([0-9\\.\\-]+)_([0-9\\.\\-]+)_([0-9\\.\\-]+)_([0-9\\.\\-]+)(_[VMmE]W)$");
    static final Pattern RPAT = Pattern.compile("([^:]+):([0-9\\.\\-]+)_([0-9\\.\\-]+)(_[VMmE][NR])?$");
    static final Pattern[] PATS = {WPAT, RPAT};

    class CustomPropertyItem implements Item {

        private String key;
        private String category, name, description;
        private String value;

        public CustomPropertyItem(String name, String category, String description) {
            this.name = name;
            this.category = category;
            this.description = description;
        }

        @Override
        public Class<?> getType() {
            return String.class;
        }

        @Override
        public String getCategory() {
            return category;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return "test";
        }

        @Override
        public Object getValue() {
            return value;
        }

        @Override
        public void setValue(Object value) {
            if (value == null) {
                this.value = "";
            } else {
                this.value = value.toString();
            }
        }

        @Override
        public Optional<ObservableValue<? extends Object>> getObservableValue() {
            return Optional.empty();
        }
    }

    class ChoicePropertyItem implements Item {

        private String key;
        private String category, name, description;
        private MeasureTypes value = MeasureTypes.V;

        public ChoicePropertyItem(String name, String category, String description) {
            this.name = name;
            this.category = category;
            this.description = description;
        }

        @Override
        public Class<?> getType() {
            return MeasureTypes.class;
        }

        @Override
        public String getCategory() {
            return category;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public MeasureTypes getValue() {
            return value;
        }

        @Override
        public void setValue(Object value) {
            this.value = (MeasureTypes) value;
        }

        @Override
        public Optional<ObservableValue<? extends Object>> getObservableValue() {
            return Optional.empty();
        }
    }

    class OffsetPropertyItem implements Item {

        private String key;
        private String category, name, description;
        private OffsetTypes value = OffsetTypes.N;

        public OffsetPropertyItem(String name, String category, String description) {
            this.name = name;
            this.category = category;
            this.description = description;
        }

        @Override
        public Class<?> getType() {
            return OffsetTypes.class;
        }

        @Override
        public String getCategory() {
            return category;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public OffsetTypes getValue() {
            return value;
        }

        @Override
        public void setValue(Object value) {
            this.value = (OffsetTypes) value;
        }

        @Override
        public Optional<ObservableValue<? extends Object>> getObservableValue() {
            return Optional.empty();
        }
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        scanTable = new ScanTable(this, tableView);
    }

    public static ScannerController create(FXMLController fxmlController, Stage parent, PolyChart chart) {
        FXMLLoader loader = new FXMLLoader(SpecAttrWindowController.class.getResource("/fxml/ScannerScene.fxml"));
        final ScannerController controller;
        Stage stage = new Stage(StageStyle.DECORATED);

        try {
            Scene scene = new Scene((Pane) loader.load());
            stage.setScene(scene);
            scene.getStylesheets().add("/styles/Styles.css");

            controller = loader.<ScannerController>getController();
            controller.fxmlController = fxmlController;
            controller.stage = stage;
            controller.chart = chart;
            controller.initParSheet();
            stage.setTitle("NMRFx Processor Scanner");

            stage.initOwner(parent);
            stage.show();
            stage.setOnCloseRequest(e -> controller.stageClosed());
            if (createControllerAction != null) {
                createControllerAction.accept(controller);
            }
            return controller;
        } catch (IOException ioE) {
            ioE.printStackTrace();
            System.out.println(ioE.getMessage());
        }

        return null;

    }

    public static void addCreateAction(Consumer<ScannerController> action) {
        createControllerAction = action;
    }

    void stageClosed() {
        chart.setDrawlist(Collections.EMPTY_LIST);
        stage.close();
    }

    private void initParSheet() {
        parSheet.setPropertyEditorFactory(new NvFxPropertyEditorFactory());
        parSheet.setMode(PropertySheet.Mode.CATEGORY);
        parSheet.setModeSwitcherVisible(false);
        parSheet.setSearchBoxVisible(false);
        measureItem = new ChoicePropertyItem("Mode", "Measure Parameters", "Measurement modes");
        offsetItem = new OffsetPropertyItem("Offset", "Measure Parameters", "Offset modes");
        ChangeListener stringListener = new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String string, String string2) {
            }
        };
        scanDirListener = (ObservableValue<? extends String> observableValue, String string, String string2) -> {
            scanTable.setScanDirectory(new File(string2));
        };
        outputDirListener = (ObservableValue<? extends String> observableValue, String string, String string2) -> {
            scanTable.setScanOutputDirectory(new File(string2));
        };

        outputFileItem = new TextOperationItem(stringListener, "process.nv", "File Locations", "Output File", "Output FileName");
        scanDirItem = new DirectoryOperationItem(scanDirListener, getDatasetDirectory().getPath(), "File Locations", "Scan Dir", "Directory to scan for datasets");
        outputDirItem = new DirectoryOperationItem(outputDirListener, getDatasetDirectory().getPath(), "File Locations", "Output Dir", "Directory to put output files in");

        parSheet.getItems().addAll(scanDirItem, outputDirItem, outputFileItem, measureItem, offsetItem);
    }

    @FXML
    private void processScanDirAndCombine(ActionEvent event) {
        ChartProcessor chartProcessor = fxmlController.getChartProcessor();
        scanTable.processScanDir(stage, chartProcessor, true);
        tabPane.getSelectionModel().select(1);
    }

    @FXML
    private void processScanDir(ActionEvent event) {
        ChartProcessor chartProcessor = fxmlController.getChartProcessor();
        scanTable.processScanDir(stage, chartProcessor, false);
        tabPane.getSelectionModel().select(1);
    }

    @FXML
    private void scanDirAction(ActionEvent event) {
        scanTable.loadScanFiles(stage);
        tabPane.getSelectionModel().select(1);
    }

    @FXML
    private void loadTableAction(ActionEvent event) {
        scanTable.loadScanTable();
        tabPane.getSelectionModel().select(1);
    }

    @FXML
    private void saveTableAction(ActionEvent event) {
        scanTable.saveScanTable();
    }

    @FXML
    private void freezeSort(ActionEvent event) {

    }

    @FXML
    private void purgeInactive(ActionEvent event) {
        ObservableList<FileTableItem> tempItems = FXCollections.observableArrayList();
        tempItems.addAll(tableView.getItems());
        scanTable.getItems().setAll(tempItems);
    }

    @FXML
    private void loadFromDataset(ActionEvent event) {
        scanTable.loadFromDataset();
    }

    @FXML
    private void openSelectedListFile(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            scanTable.openSelectedListFile();
        }
    }

    @FXML
    private void loadScriptTab(Event event) {
    }

    public Stage getStage() {
        return stage;
    }

    public ToolBar getToolBar() {
        return scannerBar;
    }

    public PolyChart getChart() {
        return chart;
    }

    public FXMLController getFXMLController() {
        return fxmlController;
    }

    public ScanTable getScanTable() {
        return scanTable;
    }

    public String getScanDirectory() {
        return scanDirItem.get();
    }

    public void setScanDirectory(String dirString) {
        scanDirItem.setFromString(dirString);
    }

    public void updateScanDirectory(String dirString) {
        scanDirItem.setFromString(dirString);
        scanDirItem.updateEditor();
    }

    public String getOutputDirectory() {
        return outputDirItem.get();
    }

    public String getOutputFileName() {
        return outputFileItem.get();
    }

    private boolean hasColumnName(String columnName) {
        List<String> headers = scanTable.getHeaders();
        boolean result = false;
        for (String header : headers) {
            int colon = header.indexOf(":");
            if (colon != -1) {
                if (header.substring(0, colon).equals(columnName)) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    @FXML
    private void measure(ActionEvent event) {
        TextInputDialog textInput = new TextInputDialog();
        textInput.setHeaderText("New column name");
        Optional<String> columNameOpt = textInput.showAndWait();
        if (columNameOpt.isPresent()) {
            String columnName = columNameOpt.get();
            columnName = columnName.replace(':', '_').replace(' ', '_');
            if (!columnName.equals("")) {
                if (hasColumnName(columnName)) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Column exists");
                    alert.showAndWait();
                    return;
                }
            }
            DatasetBase dataset = chart.getDataset();
            double[] ppms = chart.getVerticalCrosshairPositions();
            double[] wppms = new double[2];
            wppms[0] = chart.getAxis(0).getLowerBound();
            wppms[1] = chart.getAxis(0).getUpperBound();
            int extra = 1;

            Measure measure = new Measure(columnName, 0, ppms[0], ppms[1], wppms[0], wppms[1], extra, offsetItem.getValue(), measureItem.getValue());
            String columnDescriptor = measure.getColumnDescriptor();
            String columnPrefix = scanTable.getNextColumnName(columnName, columnDescriptor);
            measure.setName(columnPrefix);
            String newColumnName = columnPrefix + ":" + columnDescriptor;
            List<Double> allValues = new ArrayList<>();
            List<FileTableItem> items = scanTable.getItems();

            for (FileTableItem item : items) {
                String datasetName = item.getDatasetName();
                Dataset itemDataset = Dataset.getDataset(datasetName);
                if (itemDataset == null) {
                    File datasetFile = new File(scanTable.getScanOutputDirectory(), datasetName);
                    try {
                        itemDataset = new Dataset(datasetFile.getPath(), datasetFile.getPath(), true, false);
                    } catch (IOException ioE) {
                        GUIUtils.warn("Measure", "Can't open dataset " + datasetFile.getPath());
                        return;
                    }
                }

                List<Double> values = measureRegion(itemDataset, measure);
                if (values == null) {
                    return;
                }
                allValues.addAll(values);
                if (allValues.size() >= items.size()) {
                    break;
                }
            }
            setItems(newColumnName, allValues);
            scanTable.addTableColumn(newColumnName, "D");
        }
    }

    private List<Double> measureRegion(Dataset dataset, Measure measure) {
        List<Double> values;
        try {
            values = measure.measure(dataset);
        } catch (IOException ex) {
            Logger.getLogger(ScannerController.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        return values;
    }

    public void addColumnData(String newColumnName, List<Double> values) {
        setItems(newColumnName, values);
        scanTable.addTableColumn(newColumnName, "D");
    }

    public void setItems(String columnName, List<Double> values) {
        ObservableList<FileTableItem> items = scanTable.getItems();
        Map<Integer, FileTableItem> map = new HashMap<>();
        for (FileTableItem item : items) {
            if (item.getRow() > 0) {
                // rows index from 1
                map.put(item.getRow() - 1, item);
            }
        }
        for (int i = 0; i < values.size(); i++) {
            double value = values.get(i);
            FileTableItem item = map.get(i);
            if (item != null) {
                item.setExtra(columnName, value);
            }
        }
    }

    public List<Double> getValues(String columnName) {
        ObservableList<FileTableItem> items = scanTable.getItems();
        Map<Integer, FileTableItem> map = new HashMap<>();
        List<Double> values = new ArrayList<>(items.size());
        values.addAll(Collections.nCopies(items.size(), 0.0));
        for (FileTableItem item : items) {
            if (item.getRow() > 0) {
                int row = item.getRow() - 1;
                double value = item.getDoubleExtra(columnName);
                values.set(row, value);
            }
        }
        return values;
    }

    public boolean hasColumn(String columnName) {
        return scanTable.getHeaders().contains(columnName);
    }

    @FXML
    void showTRACTGUI() {
        if (tractGUI == null) {
            tractGUI = new TRACTGUI(this);

        }
        tractGUI.showMCplot();
    }

    @FXML
    void measureRegions() {
        DatasetBase dataset = chart.getDataset();
        List<String> headers = scanTable.getHeaders();
        for (String header : headers) {
            Optional<Measure> measureOpt = matchHeader(header);
            if (measureOpt.isPresent()) {
                try {
                    List<Double> values = measureOpt.get().measure(dataset);
                    setItems(header, values);
                } catch (IOException ex) {
                    Logger.getLogger(ScannerController.class.getName()).log(Level.SEVERE, null, ex);
                    return;
                }
            }
        }
        scanTable.refresh();
    }

    @FXML
    void showRegions() {
        DatasetBase dataset = chart.getDataset();
        List<String> headers = scanTable.getHeaders();
        TreeSet<DatasetRegion> regions = new TreeSet<>();

        for (String header : headers) {
            Optional<Measure> measureOpt = matchHeader(header);
            if (measureOpt.isPresent()) {
                Measure measure = measureOpt.get();
                DatasetRegion region = new DatasetRegion(measure.ppm1, measure.ppm2);
                regions.add(region);
            }
        }
        dataset.setRegions(regions);
        chart.chartProps.setRegions(true);
        chart.chartProps.setIntegrals(false);
        chart.refresh();
    }

    @FXML
    void clearRegions() {
        DatasetBase dataset = chart.getDataset();
        TreeSet<DatasetRegion> regions = new TreeSet<>();

        dataset.setRegions(regions);
        chart.chartProps.setRegions(false);
        chart.refresh();
    }

    @FXML
    void loadRegions() {
        FileChooser chooser = new FileChooser();
        File file = chooser.showOpenDialog(null);
        if (file != null) {
            try (BufferedReader reader = Files.newBufferedReader(file.toPath())) {
                while (true) {
                    String s = reader.readLine();
                    if (s == null) {
                        break;
                    }
                    String[] fields = s.split(" +");
                    if (fields.length > 2) {
                        String name = fields[0];
                        StringBuilder sBuilder = new StringBuilder();
                        for (int i = 1; i < fields.length; i++) {
                            sBuilder.append(fields[i]);
                            if (i != fields.length - 1) {
                                sBuilder.append("_");
                            }
                        }
                        String columnPrefix;
                        if (name.startsWith("V.")) {
                            columnPrefix = scanTable.getNextColumnName("", sBuilder.toString());
                        } else {
                            columnPrefix = name;
                        }
                        sBuilder.insert(0, ':');
                        sBuilder.insert(0, columnPrefix);
                        scanTable.addTableColumn(sBuilder.toString(), "D");
                    }
                }
            } catch (IOException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("Couldn't read file");
                alert.showAndWait();
            }

        }
    }

    @FXML
    void saveRegions() {
        FileChooser chooser = new FileChooser();
        File file = chooser.showSaveDialog(null);
        if (file != null) {
            try {
                file.createNewFile();
                try (FileWriter writer = new FileWriter(file)) {
                    List<String> headers = scanTable.getHeaders();
                    for (String header : headers) {
                        Optional<Measure> measure = matchHeader(header);
                        if (measure.isPresent()) {
                            writer.write(measure.get().getFileString());
                            writer.write('\n');
                        }
                    }
                }
            } catch (IOException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("Couldn't save file");
                alert.showAndWait();
            }
        }
    }

    public static Optional<Measure> matchHeader(String header) {
        Optional<Measure> result = Optional.empty();
        String columnName;
        String oMode;
        String mMode;
        String group = "_VN";
        double ppm1;
        double ppm2;
        double ppmw1;
        double ppmw2;
        for (Pattern pat : PATS) {
            Matcher matcher = pat.matcher(header);
            if (matcher.matches()) {
                columnName = matcher.group(1);
                int nGroups = matcher.groupCount();
                ppm1 = Double.parseDouble(matcher.group(2));
                ppm2 = Double.parseDouble(matcher.group(3));
                if (nGroups == 6) {
                    ppmw1 = Double.parseDouble(matcher.group(4));
                    ppmw2 = Double.parseDouble(matcher.group(5));
                } else {
                    ppmw1 = ppm1;
                    ppmw2 = ppm2;
                }

                if (nGroups >= 4) {
                    String lastGroup = matcher.group(nGroups);
                    if (lastGroup != null) {
                        group = lastGroup;
                    }
                }

                mMode = group.substring(1, 2);
                oMode = group.substring(2, 3);

                OffsetTypes oType = OffsetTypes.valueOf(oMode);
                MeasureTypes mType = MeasureTypes.valueOf(mMode);
                Measure measure = new Measure(columnName, 0, ppm1, ppm2, ppmw1, ppmw2,
                        0, oType, mType);
                result = Optional.of(measure);
                break;
            }
        }
        return result;

    }
}
