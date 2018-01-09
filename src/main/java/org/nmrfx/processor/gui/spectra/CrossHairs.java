/*
 * NMRFx Processor : A Program for Processing NMR Data 
 * Copyright (C) 2004-2017 One Moon Scientific, Inc., Westfield, N.J., USA
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.nmrfx.processor.gui.spectra;

import java.util.List;
import javafx.scene.shape.Line;
import org.nmrfx.processor.datasets.Dataset;
import org.nmrfx.processor.gui.FXMLController;
import org.nmrfx.processor.gui.PolyChart;
import static org.nmrfx.processor.gui.PolyChart.CROSSHAIR_TOL;
import static org.nmrfx.processor.gui.PolyChart.HORIZONTAL;
import static org.nmrfx.processor.gui.PolyChart.VERTICAL;
import org.nmrfx.processor.gui.SpectrumStatusBar;

/**
 *
 * @author Bruce Johnson
 */
public class CrossHairs {

    final PolyChart chart;
    FXMLController controller = null;
    final NMRAxis xAxis;
    final NMRAxis yAxis;
    double[][] crossHairPositions;
    boolean[][] crossHairStates;
    Line[][] crossHairLines = new Line[2][2];

    public CrossHairs(PolyChart chart, double[][] crossHairPositions, boolean[][] crossHairStates, Line[][] crossHairLines) {
        this.chart = chart;
        xAxis = chart.getAxis(0);
        yAxis = chart.getAxis(1);
        this.crossHairPositions = crossHairPositions;
        this.crossHairStates = crossHairStates;
        this.crossHairLines = crossHairLines;
    }

    private FXMLController getController() {
        if (controller == null) {
            controller = chart.getController();
        }
        return controller;
    }

    public void hideCrossHairs() {
        SpectrumStatusBar statusBar = getController().getStatusBar();
        for (int iCross = 0; iCross < 2; iCross++) {
            for (int jOrient = 0; jOrient < 2; jOrient++) {
                crossHairLines[iCross][jOrient].setVisible(false);
                int iAxis = jOrient == 0 ? 1 : 0;
                double value = iCross == 1 ? chart.getAxis(iAxis).getLowerBound() : chart.getAxis(iAxis).getUpperBound();
                statusBar.setCrossText(jOrient, iCross, value, true);
            }
        }
    }

    public void setCrossHairState(boolean value) {
        for (int iCross = 0; iCross < 2; iCross++) {
            for (int jOrient = 0; jOrient < 2; jOrient++) {
                crossHairStates[iCross][jOrient] = value;
            }
        }
        if (!value) {
            hideCrossHairs();
        }
    }

    public void refreshCrossHairs() {
        SpectrumStatusBar statusBar = getController().getStatusBar();
        for (int iCross = 0; iCross < 2; iCross++) {
            for (int jOrient = 0; jOrient < 2; jOrient++) {
                int iAxis = jOrient == 0 ? 1 : 0;
                NMRAxis axis = chart.getAxis(iAxis);
                if (crossHairStates[iCross][jOrient] && crossHairLines[iCross][jOrient].isVisible()) {
                    drawCrossHair(iCross, jOrient);
                } else {
                    double value = iCross == 1 ? axis.getLowerBound() : axis.getUpperBound();
                    statusBar.setCrossText(jOrient, iCross, value, true);
                }
                statusBar.setCrossTextRange(iCross, jOrient, axis.getLowerBound(), axis.getUpperBound());
            }
        }
        chart.drawSlices();
    }

    private static void updateAllCharts(PolyChart source, int iCross, int iOrient, double position, String dimLabel) {
        PolyChart.charts.stream().filter((c) -> (c != source)).forEach((c) -> {
            c.getCrossHairs().syncCrosshair(iCross, iOrient, dimLabel, position);
        });
    }

    public void moveCrosshair(int iCross, int iOrient, double value) {
        List<DatasetAttributes> dataAttrs = chart.getDatasetAttributes();
        if (dataAttrs.isEmpty()) {
            return;
        }
        value = crossHairInRange(iCross, iOrient, value);
        setCrossHairPosition(iCross, iOrient, value);
        drawCrossHair(iCross, iOrient);
        double aValue = crossHairPositions[iCross][iOrient];
        DatasetAttributes dataAttr = dataAttrs.get(0);
        String label;
        int axisDim = iOrient == VERTICAL ? 0 : 1;
        label = dataAttr.getLabel(axisDim);
        updateAllCharts(chart, iCross, iOrient, aValue, label);
        chart.drawSlices();
    }

    public void syncCrosshair(int iCross, int iOrient, String dimLabel, double value) {
        List<DatasetAttributes> dataAttrs = chart.getDatasetAttributes();
        if (dataAttrs.isEmpty()) {
            return;
        }
        DatasetAttributes dataAttr = dataAttrs.get(0);
        int jOrient = -1;
        if (dataAttr.getLabel(0).equals(dimLabel)) {
            if (value >= xAxis.getLowerBound() && (value <= xAxis.getUpperBound())) {
                jOrient = VERTICAL;
            }
        } else if (dataAttr.getLabel(1).equals(dimLabel)) {
            if (value >= yAxis.getLowerBound() && (value <= yAxis.getUpperBound())) {
                jOrient = HORIZONTAL;
            }
        }
        if (jOrient >= 0) {
            crossHairPositions[iCross][jOrient] = value;
            drawCrossHair(iCross, jOrient);
            if (iCross == 0) {
                chart.drawSlices();
            }
        }
    }

    public double crossHairInRange(int iCross, int iOrient, double value) {
        if (value < 0) {
            value = 1;
        }
        double width = xAxis.getWidth();
        double height = yAxis.getHeight();

        if (iOrient == HORIZONTAL) {
            if (value > height) {
                value = height - 1;
            }
        } else if (value > width) {
            value = width - 1;
        }
        return value;

    }

    public void setCrossHairPosition(int iCross, int iOrient, double value) {
        if (iOrient == HORIZONTAL) {
            value = yAxis.getValueForDisplay(value).doubleValue();
        } else {
            value = xAxis.getValueForDisplay(value).doubleValue();
        }
        crossHairPositions[iCross][iOrient] = value;
    }

    public boolean hasCrosshairRegion() {
        boolean horizontalRegion = crossHairStates[0][VERTICAL] && crossHairLines[0][VERTICAL].isVisible()
                && crossHairStates[1][VERTICAL] && crossHairLines[1][VERTICAL].isVisible();
        boolean verticalRegion = crossHairStates[0][HORIZONTAL] && crossHairLines[0][HORIZONTAL].isVisible()
                && crossHairStates[1][HORIZONTAL] && crossHairLines[1][HORIZONTAL].isVisible();
        boolean hasRegion;
        if (chart.is1D()) {
            hasRegion = horizontalRegion;
        } else {
            hasRegion = horizontalRegion && verticalRegion;
        }
        return hasRegion;
    }

    public int getCrossHairNum(double x, double y, int iOrient) {
        int crossHairNum = 0;
        if (crossHairStates[1][iOrient] && crossHairLines[1][iOrient].isVisible()) {
            if (iOrient == HORIZONTAL) {
                double delta0 = Math.abs(crossHairLines[0][iOrient].getStartY() - y);
                double delta1 = Math.abs(crossHairLines[1][iOrient].getStartY() - y);
                if (delta1 < delta0) {
                    crossHairNum = 1;
                }
            } else {
                double delta0 = Math.abs(crossHairLines[0][iOrient].getStartX() - x);
                double delta1 = Math.abs(crossHairLines[1][iOrient].getStartX() - x);
                if (delta1 < delta0) {
                    crossHairNum = 1;
                }
            }
        } else if (!crossHairLines[0][iOrient].isVisible()) {
            crossHairNum = 0;
        } else if (iOrient == HORIZONTAL) {
            double delta0 = Math.abs(crossHairLines[0][iOrient].getStartY() - y);
            if (delta0 > CROSSHAIR_TOL) {
                crossHairNum = 1;
            }
        } else {
            double delta0 = Math.abs(crossHairLines[0][iOrient].getStartX() - x);
            if (delta0 > CROSSHAIR_TOL) {
                crossHairNum = 1;
            }
        }
        return crossHairNum;
    }

    public void setSliceStatus(boolean state) {
        refreshCrossHairs();
    }

    public void drawCrossHair(int iCross, int iOrient) {
        Dataset dataset = chart.getDataset();
        if (dataset == null) {
            return;
        }
        double width = xAxis.getWidth();
        double height = yAxis.getHeight();
        if (crossHairStates[iCross][iOrient]) {
            double value = crossHairPositions[iCross][iOrient];
            getController().getStatusBar().setCrossText(iOrient, iCross, value, false);
            if (iOrient == HORIZONTAL) {
                value = yAxis.getDisplayPosition(value);
                crossHairLines[iCross][iOrient].setStartX(0);
                crossHairLines[iCross][iOrient].setEndX(width);
                crossHairLines[iCross][iOrient].setStartY(value);
                crossHairLines[iCross][iOrient].setEndY(value);
            } else {
                value = xAxis.getDisplayPosition(value);
                crossHairLines[iCross][iOrient].setStartY(0);
                crossHairLines[iCross][iOrient].setEndY(height);
                crossHairLines[iCross][iOrient].setStartX(value);
                crossHairLines[iCross][iOrient].setEndX(value);
            }
            crossHairLines[iCross][iOrient].setVisible(true);
            crossHairLines[iCross][iOrient].setVisible(true);
        }
    }

}