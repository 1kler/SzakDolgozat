package com.example.onepix;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class CanvasPane {

    @FXML
    private AnchorPane anchorPane;
    @FXML
    private ColorPicker colorPicker;
    @FXML
    private Button undoButton;
    @FXML
    private Button saveButton;
    @FXML
    private Button loadButton;
    @FXML
    private Button fillButton;
    @FXML
    private Button pencilButton;
    @FXML
    private Button eraserButton;
    @FXML private Button zoomButton;
    @FXML
    private Slider pixelSizeSlider;
    @FXML
    private Label sliderValueLabel;

    private Canvas canvas;
    private GraphicsContext gc;
    private int onePixSize = 10;
    private int drawingPixelSize;
    private int drawSizeMultiplier = 1;
    private static final int UNDO_STACK_LIMIT = 20;
    private int[] lastPixel = {-1, -1};

    private double zoomLevel = 1.0;
    private final double ZOOM_FACTOR = 1.1;

    private static final double MAX_ZOOM_LEVEL = 5.0;
    private static final double MIN_ZOOM_LEVEL = 1;

    private boolean pencilMode = true;
    private boolean fillMode = false;

    private boolean eraserMode = false;

    private boolean zoomMode = false;

    private Stack<WritableImage> undoStack = new Stack<>();
    private EventHandler<MouseEvent> pencilPressedHandler;
    private EventHandler<MouseEvent> pencilDraggedHandler;
    private EventHandler<MouseEvent> fillPressedHandler;

    private EventHandler<MouseEvent> highlightHandler;

    private EventHandler<ScrollEvent> zoomHandler;

    private Map<Point, Color> originalPixelColors = new HashMap<>();


    @FXML
    public void initialize() {
        pixelSizeSlider.setMin(1);
        pixelSizeSlider.setMax(10); 
        pixelSizeSlider.setValue(1); 
        pixelSizeSlider.setMajorTickUnit(1);
        pixelSizeSlider.setMinorTickCount(0);
        pixelSizeSlider.setSnapToTicks(true);

        drawSizeMultiplier = (int) pixelSizeSlider.getValue();
        drawingPixelSize = onePixSize * drawSizeMultiplier;


        pixelSizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            drawSizeMultiplier = newVal.intValue();
            drawingPixelSize = onePixSize * drawSizeMultiplier;
            sliderValueLabel.setText(String.valueOf(drawSizeMultiplier)); 
        });
    }

    public void setCanvasSize(double height, double width) {
        canvas = new Canvas(width * onePixSize, height * onePixSize);
        gc = canvas.getGraphicsContext2D();

        Pane canvasContainer = new Pane();
        canvasContainer.setPrefSize(width * onePixSize, height * onePixSize);
        canvasContainer.setStyle("-fx-background-color: white;");

        canvasContainer.getChildren().add(canvas);

        double topAnchor = (anchorPane.getPrefHeight() - height * onePixSize) / 2;
        double leftAnchor = (anchorPane.getPrefWidth() - width * onePixSize) / 2;
        AnchorPane.setTopAnchor(canvasContainer, topAnchor);
        AnchorPane.setLeftAnchor(canvasContainer, leftAnchor);
        AnchorPane.setRightAnchor(canvasContainer, leftAnchor);
        AnchorPane.setBottomAnchor(canvasContainer, topAnchor);

        anchorPane.getChildren().add(canvasContainer);

        setupDrawing();
        setupUndoButton();
        setupSaveButton();
        setupLoadButton();
        setupPencilButton();
        setupFillButton();
        setupEraserButton();
        setupZoomButton();
    }

    private void resetMouseEvents() {
        if (pencilPressedHandler != null) {
            canvas.removeEventHandler(MouseEvent.MOUSE_PRESSED, pencilPressedHandler);
        }
        if (pencilDraggedHandler != null) {
            canvas.removeEventHandler(MouseEvent.MOUSE_DRAGGED, pencilDraggedHandler);
        }
        if (fillPressedHandler != null) {
            canvas.removeEventHandler(MouseEvent.MOUSE_PRESSED, fillPressedHandler);
        }
        if (highlightHandler != null) {
            canvas.removeEventHandler(MouseEvent.MOUSE_MOVED, highlightHandler);
        }
        if (zoomHandler != null) {
            canvas.removeEventHandler(ScrollEvent.SCROLL, zoomHandler);
        }
    }
    private void setupDrawing() {
        resetMouseEvents();

        if (pencilMode) {
            createPencilHandlers();
            handleMouseHover(canvas);
            canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, pencilPressedHandler);
            canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, pencilDraggedHandler);
            canvas.addEventHandler(MouseEvent.MOUSE_MOVED,highlightHandler);
        } else if (fillMode) {
            createFillHandlers();
            canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, fillPressedHandler);
        }else if (eraserMode) {
            createEraserHandlers();
            handleMouseHover(canvas);
            canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, pencilPressedHandler);
            canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, pencilDraggedHandler);
            canvas.addEventHandler(MouseEvent.MOUSE_MOVED,highlightHandler);
        }
        else if(zoomMode){
            createZoomHandlers();
            canvas.addEventHandler(ScrollEvent.SCROLL,zoomHandler);
        }
    }

    private void createPencilHandlers() {
        pencilPressedHandler = event -> {
            gc.setFill(colorPicker.getValue());
            canvas.removeEventHandler(MouseEvent.MOUSE_MOVED,highlightHandler);
            saveSnapshot();
            snapAndDraw(event.getX(), event.getY());
            originalPixelColors.clear();
            restoreOriginalColors();
            canvas.addEventHandler(MouseEvent.MOUSE_MOVED,highlightHandler);
            redrawCanvas();
        };

        pencilDraggedHandler = event -> {
            canvas.removeEventHandler(MouseEvent.MOUSE_MOVED,highlightHandler);
            originalPixelColors.clear();
            restoreOriginalColors();
            snapAndDraw(event.getX(), event.getY());
            canvas.addEventHandler(MouseEvent.MOUSE_MOVED,highlightHandler);
            redrawCanvas();
        };
    }

    private void createFillHandlers() {
        fillPressedHandler = event -> {
            gc.setFill(colorPicker.getValue());
            saveSnapshot();
            crossFill(event.getX(), event.getY());
            redrawCanvas();
        };
    }

    private void createZoomHandlers() {
        zoomHandler = event -> {
            if (event.getDeltaY() > 0) {
                zoomIn();
            } else {
                zoomOut();
            }
            event.consume();
        };
    }

    private void zoomIn() {
        if (zoomLevel < MAX_ZOOM_LEVEL) {
            zoomLevel *= ZOOM_FACTOR;
            applyZoom();
        }
    }

    private void zoomOut() {
        if (zoomLevel > MIN_ZOOM_LEVEL) {
            zoomLevel /= ZOOM_FACTOR;
            applyZoom();
        }
    }

    private void applyZoom() {
        double canvasWidth = canvas.getWidth();
        double canvasHeight = canvas.getHeight();

        double centerX = canvasWidth / 2;
        double centerY = canvasHeight / 2;

        canvas.setScaleX(zoomLevel);
        canvas.setScaleY(zoomLevel);

        double translateX = centerX - (centerX * zoomLevel);
        double translateY = centerY - (centerY * zoomLevel);

        canvas.setTranslateX(translateX);
        canvas.setTranslateY(translateY);
    }

    private void handleMouseHover(Canvas canvas) {

        highlightHandler = event -> {

            int snappedX = (int) (event.getX() / onePixSize);
            int snappedY = (int) (event.getY() / onePixSize);


            if (snappedX < 0 || snappedY < 0 || snappedX >= canvas.getWidth() / onePixSize || snappedY >= canvas.getHeight() / onePixSize) {
                return; 
            }


            if (lastPixel[0] != snappedX || lastPixel[1] != snappedY) {
                restoreOriginalColors();
                originalPixelColors.clear();


                int widthInPixels = drawingPixelSize / onePixSize;
                int heightInPixels = drawingPixelSize / onePixSize;


                for (int x = snappedX; x < snappedX + widthInPixels; x++) {
                    for (int y = snappedY; y < snappedY + heightInPixels; y++) {

                        if ( x < canvas.getWidth() / onePixSize && y < canvas.getHeight() / onePixSize) {

                            Color pixelColor = getPixelColor(x * onePixSize, y * onePixSize);
                            originalPixelColors.put(new Point(x, y), pixelColor);


                            gc.setFill(eraserMode ? Color.WHITE : colorPicker.getValue());
                            gc.fillRect(x * onePixSize, y * onePixSize, onePixSize, onePixSize);
                        }
                    }
                }
                lastPixel[0] = snappedX;
                lastPixel[1] = snappedY;
            }
        };

        canvas.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            restoreOriginalColors();
        });
    }


    private void restoreOriginalColors() {
        for (Map.Entry<Point, Color> entry : originalPixelColors.entrySet()) {
            Point pixel = entry.getKey();
            Color originalColor = entry.getValue();

            gc.setFill(originalColor);
            gc.fillRect(pixel.x * onePixSize, pixel.y * onePixSize, onePixSize, onePixSize);
        }
        originalPixelColors.clear();
    }

    private void createEraserHandlers() {
        pencilPressedHandler = event -> {
            gc.setFill(Color.WHITE);
            saveSnapshot();
            snapAndDraw(event.getX(), event.getY());
            canvas.removeEventHandler(MouseEvent.MOUSE_MOVED,highlightHandler);
            originalPixelColors.clear();
            restoreOriginalColors();
            canvas.addEventHandler(MouseEvent.MOUSE_MOVED,highlightHandler);
            redrawCanvas();
        };

        pencilDraggedHandler = event -> {
            canvas.removeEventHandler(MouseEvent.MOUSE_MOVED,highlightHandler);
            originalPixelColors.clear();
            restoreOriginalColors();
            snapAndDraw(event.getX(), event.getY());
            canvas.addEventHandler(MouseEvent.MOUSE_MOVED,highlightHandler);
            redrawCanvas();
        };
    }


    private void snapAndDraw(double x, double y) {

        double adjustedX = (x - canvas.getTranslateX()) / zoomLevel;
        double adjustedY = (y - canvas.getTranslateY()) / zoomLevel;

        int snappedX = (int) (adjustedX / onePixSize) * onePixSize;
        int snappedY = (int) (adjustedY / onePixSize) * onePixSize;

        double scaledDrawingPixelSize = drawingPixelSize / zoomLevel;
        gc.fillRect(snappedX, snappedY, scaledDrawingPixelSize, scaledDrawingPixelSize);
    }





    private void crossFill(double x, double y) {
        int snappedX = (int) (x / onePixSize);
        int snappedY = (int) (y / onePixSize);


        Color targetColor = getPixelColor((int) x, (int) y);
        if (targetColor.equals(colorPicker.getValue())) {
            return;
        }

        Color selectedColor = colorPicker.getValue();

        boolean[][] visited = new boolean[(int) (canvas.getWidth() / onePixSize)][(int) (canvas.getHeight() / onePixSize)];

        Queue<Point> queue = new LinkedList<>();
        queue.add(new Point(snappedX, snappedY));
        visited[snappedX][snappedY] = true;

        while (!queue.isEmpty()) {
            Point p = queue.poll();
            int xCoord = p.x;
            int yCoord = p.y;

            
            gc.setFill(selectedColor);
            gc.fillRect(xCoord * onePixSize, yCoord * onePixSize, onePixSize, onePixSize);

            
            if (xCoord + 1 < canvas.getWidth() / onePixSize && !visited[xCoord + 1][yCoord] &&
                    getPixelColor((xCoord + 1) * onePixSize + 1, yCoord * onePixSize + 1).equals(targetColor)) {
                queue.add(new Point(xCoord + 1, yCoord));
                visited[xCoord + 1][yCoord] = true;
            }
            if (xCoord - 1 >= 0 && !visited[xCoord - 1][yCoord] &&
                    getPixelColor((xCoord - 1) * onePixSize + 1, yCoord * onePixSize + 1).equals(targetColor)) {
                queue.add(new Point(xCoord - 1, yCoord));
                visited[xCoord - 1][yCoord] = true;
            }
            if (yCoord + 1 < canvas.getHeight() / onePixSize && !visited[xCoord][yCoord + 1] &&
                    getPixelColor(xCoord * onePixSize + 1, (yCoord + 1) * onePixSize + 1).equals(targetColor)) {
                queue.add(new Point(xCoord, yCoord + 1));
                visited[xCoord][yCoord + 1] = true;
            }
            if (yCoord - 1 >= 0 && !visited[xCoord][yCoord - 1] &&
                    getPixelColor(xCoord * onePixSize + 1, (yCoord - 1) * onePixSize + 1).equals(targetColor)) {
                queue.add(new Point(xCoord, yCoord - 1));
                visited[xCoord][yCoord - 1] = true;
            }
        }
    }

    private Color getPixelColor(int x, int y) {
        WritableImage snapshot = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
        canvas.snapshot(null, snapshot);
        PixelReader pixelReader = snapshot.getPixelReader();
        return pixelReader.getColor(x, y);
    }

    private void redrawCanvas() {
        WritableImage savedImage = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
        canvas.snapshot(null, savedImage);

        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.drawImage(savedImage, 0, 0);

        
        canvas.setScaleX(zoomLevel);
        canvas.setScaleY(zoomLevel);
    }


    private void saveSnapshot() {
        WritableImage snapshot = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
        canvas.snapshot(new SnapshotParameters(), snapshot);
        undoStack.push(snapshot);
        if (undoStack.size() > UNDO_STACK_LIMIT) {
            undoStack.remove(0);  
        }
    }

    private void setupUndoButton() {
        undoButton.setOnAction(event -> undo());
    }

    private void undo() {
        if (!undoStack.isEmpty()) {
            WritableImage snapshot = undoStack.pop();
            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            gc.drawImage(snapshot, 0, 0);
        }
    }

    private void setupSaveButton() {
        saveButton.setOnAction(event -> {
            try {
                saveImage();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void setupLoadButton() {
        loadButton.setOnAction(event -> {
            try {
                loadImage();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void setupPencilButton() {
        pencilButton.setOnAction(event -> {
            pencilMode = true;
            fillMode = false;
            eraserMode = false;
            zoomMode = false;
            setupDrawing();
        });
    }
    private void setupFillButton() {
        fillButton.setOnAction(event -> {
            fillMode = true;
            pencilMode = false;
            eraserMode = false;
            zoomMode = false;
            setupDrawing();
        });
    }

    private void setupEraserButton() {
        eraserButton.setOnAction(event -> {
            pencilMode = false;
            fillMode = false;
            zoomMode = false;
            eraserMode = true;
            setupDrawing();
        });
    }

    private void setupZoomButton() {
        zoomButton.setOnAction(event -> {
            pencilMode = false;
            fillMode = false;
            eraserMode = false;
            zoomMode = true;
            setupDrawing();
        });
    }


    private void loadImage() throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG files (*.png)", "*.png"));
        File file = fileChooser.showOpenDialog(canvas.getScene().getWindow());
        if (file != null) {
            try {
                javafx.scene.image.Image image = new javafx.scene.image.Image(file.toURI().toString());
                PixelReader pixelReader = image.getPixelReader();
                saveSnapshot(); 
                gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight()); 

                double imageWidth = image.getWidth();
                double imageHeight = image.getHeight();

                
                for (int y = 0; y < imageHeight; y++) {
                    for (int x = 0; x < imageWidth; x++) {
                        Color color = pixelReader.getColor(x, y);

                        gc.setFill(color);
                        gc.fillRect(x, y, 1, 1);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }


    @FXML
    private void saveImage() throws IOException {
        
        WritableImage originalImage = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
        canvas.snapshot(null, originalImage); 

        double minX = canvas.getWidth();
        double minY = canvas.getHeight();
        double maxX = 0;
        double maxY = 0;

        PixelReader pixelReader = originalImage.getPixelReader();
        boolean contentDrawn = false;

        
        for (int y = 0; y < canvas.getHeight(); y++) {
            for (int x = 0; x < canvas.getWidth(); x++) {
                Color color = pixelReader.getColor(x, y);
                if (!color.equals(Color.TRANSPARENT) && !color.equals(Color.WHITE) && !color.equals(Color.LIGHTGRAY)) {
                    contentDrawn = true;
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }

        
        if (!contentDrawn) {
            showAlert("No Content", "No content drawn on the canvas.");
            return;
        }

        double drawnWidth = maxX - minX + 1;
        double drawnHeight = maxY - minY + 1;

        if (drawnWidth <= 0 || drawnHeight <= 0) {
            showAlert("Invalid Dimensions", "Invalid dimensions for the drawn content.");
            return;
        }

        
        WritableImage drawnImage = new WritableImage((int) drawnWidth, (int) drawnHeight);
        PixelWriter pixelWriter = drawnImage.getPixelWriter();

        for (int y = 0; y < drawnHeight; y++) {
            for (int x = 0; x < drawnWidth; x++) {
                Color color = pixelReader.getColor((int) (minX + x), (int) (minY + y));

                pixelWriter.setColor(x, y, color);
            }
        }

        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Canvas Image");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Files", "*.png"));
        File file = fileChooser.showSaveDialog(anchorPane.getScene().getWindow());

        if (file != null) {
            try {
                BufferedImage bufferedImage = SwingFXUtils.fromFXImage(drawnImage, null);
                ImageIO.write(bufferedImage, "png", file);
                System.out.println("Canvas saved to: " + file.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}