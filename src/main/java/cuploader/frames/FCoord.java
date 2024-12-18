package cuploader.frames;

import cuploader.Coord;
import cuploader.Data;
import cuploader.ImmutableCoordinate;
import cuploader.jxmapviewer.GoogleMapsTileFactoryInfo;
import cuploader.jxmapviewer.MapyCZTileFactoryInfo;
import javax.swing.*;
import javax.swing.event.MouseInputListener;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.input.CenterMapListener;
import org.jxmapviewer.input.PanKeyListener;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCursor;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.TileFactoryInfo;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.ResourceBundle;
import org.jxmapviewer.VirtualEarthTileFactoryInfo;

public class FCoord extends javax.swing.JFrame {
    private int number;
    private boolean multiEdit;
    private JPanel map;
    private JXMapViewer mapViewer = new JXMapViewer();
        
    private JXMapViewer initMap() {

        TileFactoryInfo info = new OSMTileFactoryInfo();
        DefaultTileFactory tileFactory = new DefaultTileFactory(info);
        mapViewer.setTileFactory(tileFactory);

        tileFactory.setThreadPoolSize(8);

        MouseInputListener mia = new PanMouseInputListener(mapViewer);
        mapViewer.addMouseListener(mia);
        mapViewer.addMouseMotionListener(mia);
        mapViewer.addMouseListener(new CenterMapListener(mapViewer));
        mapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCursor(mapViewer));
        mapViewer.addKeyListener(new PanKeyListener(mapViewer));

        MapMarkerListener clickListener = new MapMarkerListener(this);
        mapViewer.addMouseListener(clickListener);
        mapViewer.setOverlayPainter(clickListener);
        this.addKeyListener(clickListener);
        return mapViewer;
    }

    private int getMapZoom() {
        return mapViewer.getZoom();
    }

    private ImmutableCoordinate getMapPosition() {
        GeoPosition centerPosition = mapViewer.getCenterPosition();
        double latitude = centerPosition.getLatitude();
        double longitude = centerPosition.getLongitude();
        return new ImmutableCoordinate(latitude, longitude, "");
    }

    private Point getPoint(ImmutableCoordinate coordinate) {
        GeoPosition pos = new GeoPosition(coordinate.getLat(), coordinate.getLon());
        Point2D point2D = mapViewer.convertGeoPositionToPoint(pos);
        Point point = new Point();
        point.setLocation(point2D);
        return point;
    }

    private ImmutableCoordinate getMapPosition(Point point, Point headingPoint) {
        GeoPosition position = mapViewer.convertPointToGeoPosition(point);
        String heading = "";
        if (headingPoint != null) {
            GeoPosition headingPosition = mapViewer.convertPointToGeoPosition(headingPoint);
            int x = headingPoint.x - point.x;
            int y = headingPoint.y - point.y;
            heading = Long.toString(Math.round(Math.atan2(y, x)*180/Math.PI+90+360)%360);
        }
        return new ImmutableCoordinate(position.getLatitude(), position.getLongitude(), heading);
    }

    private void setMapPosition(ImmutableCoordinate coor, int coorZoom) {
        GeoPosition position = new GeoPosition(coor.getLat(), coor.getLon());
        mapViewer.setZoom(coorZoom);
        mapViewer.setAddressLocation(position);
    }
    
    private static class MapMarkerListener implements Painter<JXMapViewer>, MouseListener, KeyListener {

        private final FCoord fCoord;
        private ImmutableCoordinate coordinate;
        private boolean shiftPress;

        public MapMarkerListener(FCoord fCoord) {
            this.fCoord = fCoord;
            this.shiftPress = false;
        }
        
        private final int ARR_SIZE = 14;
        
        void drawArrow(Graphics g1, int x1, int y1, int x2, int y2, int w, int s, Color frameColor, Color fillColor) {
            Graphics2D g = (Graphics2D) g1.create();

            double dx = x2 - x1, dy = y2 - y1;
            double angle = Math.atan2(dy, dx);
            int len = (int) Math.sqrt(dx*dx + dy*dy);
            drawArrow(g, x1, y1, angle, len, w, s, frameColor, frameColor);
        }
        
        void drawArrow(Graphics g1, int x1, int y1, double angle, int len, int w, int s, Color frameColor, Color fillColor) {
            Graphics2D g = (Graphics2D) g1.create();

            AffineTransform at = AffineTransform.getTranslateInstance(x1, y1);
            at.concatenate(AffineTransform.getRotateInstance(angle));
            g.transform(at);

            // Draw horizontal arrow starting in (0, 0)
            Shape rc = new Rectangle2D.Float(s, -w/2, len-ARR_SIZE-s, w);
            g.setColor(frameColor);
            g.draw(rc);
            g.fill(rc);
            g.setColor(fillColor);
            g.fillPolygon(new int[] {len, len-ARR_SIZE, len-ARR_SIZE, len},
                          new int[] {0, -ARR_SIZE, ARR_SIZE, 0}, 4);
        }

        @Override
        public void paint(Graphics2D g, JXMapViewer jxMapViewer, int a, int b) {
            if (coordinate != null) {
                Color fillColor = new Color(128, 192, 255, 128);
                Color frameColor = new Color(0, 0, 255, 128);
                Point point = fCoord.getPoint(coordinate);
                int size = ARR_SIZE;
                int x = point.x - size / 2;
                int y = point.y - size / 2;

                Shape rc = new Ellipse2D.Float(x, y, size, size);
                if (coordinate.getHeading() != "") {
                    drawArrow(g, point.x, point.y, (Double.parseDouble(coordinate.getHeading())-90)*Math.PI/180, 100, 4, size, frameColor, frameColor);
                }
                g.setColor(frameColor);
                g.draw(rc);
                g.setColor(fillColor);
                g.fill(rc);
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            fCoord.requestFocus();
            if (e.getButton() == MouseEvent.BUTTON1) {
                DecimalFormat df = new DecimalFormat("#.######", DecimalFormatSymbols.getInstance(Locale.US));
                if (shiftPress && coordinate != null) {
                    coordinate = fCoord.getMapPosition(fCoord.getPoint(coordinate), e.getPoint());
                    fCoord.tCoor.setText(df.format(coordinate.getLat()) + ";" + df.format(coordinate.getLon()) + ";" + coordinate.getHeading());
                } else {
                    coordinate = fCoord.getMapPosition(e.getPoint(), null);
                    fCoord.tCoor.setText(df.format(coordinate.getLat()) + ";" + df.format(coordinate.getLon()));
                }
                fCoord.mapViewer.repaint();
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {

        }

        @Override
        public void mouseReleased(MouseEvent e) {

        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {

        }

        @Override
        public void keyTyped(KeyEvent ke) {
            
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_SHIFT && !shiftPress) {
                shiftPress = true;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
                shiftPress = false;
            }
        }
    }

    public FCoord(int number, boolean multiEdit) {
        this.number = number;
        this.multiEdit = multiEdit;

        map = initMap();
        initComponents();
        //setResizable(false);
        setLocationRelativeTo(null);
        Coord fileCoord = Data.getFiles().get(number).coor;
        
        if(!multiEdit && fileCoord != null) {          
          setMapPosition(new ImmutableCoordinate(fileCoord.getLat(), fileCoord.getLon(), ""), 2);
          DecimalFormat df = new DecimalFormat("#.######", DecimalFormatSymbols.getInstance(Locale.US));
          GeoPosition coordinate = mapViewer.getCenterPosition();
          tCoor.setText(df.format(coordinate.getLatitude()) + ";" + df.format(coordinate.getLongitude()) + ";" + fileCoord.getHeading());
        } else if (Data.settings.coor != null && Data.settings.coorZoom != 0) {
          setMapPosition(Data.settings.coor, Data.settings.coorZoom);
        }
        setVisible(true);
        setFocusableWindowState(true);

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKeyStroke, "ESCAPE");
        getRootPane().getActionMap().put("ESCAPE", escapeAction);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        tCoor = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        bSave = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        pMap = map;
        bOSM = new javax.swing.JButton();
        bBing = new javax.swing.JButton();
        bMapquest = new javax.swing.JButton();
        bMapyCZTourist = new javax.swing.JButton();
        bMapyCZBing = new javax.swing.JButton();
        bMapyCZBase = new javax.swing.JButton();
        bMapsGoogleCOM = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("cuploader/text/messages"); // NOI18N
        setTitle(bundle.getString("file-coor")); // NOI18N

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/cuploader/resources/light-bulb.png"))); // NOI18N
        jLabel1.setText(bundle.getString("coord-info")); // NOI18N

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        jLabel3.setText("<html>"+Data.text("fileedit-coor-info")+"</html>");
        jLabel3.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        bSave.setIcon(new javax.swing.ImageIcon(getClass().getResource("/cuploader/resources/tick.png"))); // NOI18N
        bSave.setText(bundle.getString("button-apply")); // NOI18N
        bSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bSaveActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(tCoor)
                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(bSave, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, 49, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tCoor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bSave))
                .addContainerGap())
        );

        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/cuploader/resources/light-bulb.png"))); // NOI18N
        jLabel2.setText(bundle.getString("coord-info-map")); // NOI18N

        pMap.setCursor(new java.awt.Cursor(java.awt.Cursor.CROSSHAIR_CURSOR));
        pMap.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                pMapMouseReleased(evt);
            }
        });

        javax.swing.GroupLayout pMapLayout = new javax.swing.GroupLayout(pMap);
        pMap.setLayout(pMapLayout);
        pMapLayout.setHorizontalGroup(
            pMapLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        pMapLayout.setVerticalGroup(
            pMapLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 256, Short.MAX_VALUE)
        );

        bOSM.setText("OpenStreetMap");
        bOSM.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bOSMActionPerformed(evt);
            }
        });

        bBing.setText("Bing");
        bBing.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bBingActionPerformed(evt);
            }
        });

        bMapquest.setText("MapQuest Open");
        bMapquest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bMapquestActionPerformed(evt);
            }
        });

        bMapyCZTourist.setText("Mapy.cz Turist");
        bMapyCZTourist.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bMapyCZTouristActionPerformed(evt);
            }
        });

        bMapyCZBing.setText("Mapy.cz Bing");
        bMapyCZBing.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bMapyCZBingActionPerformed(evt);
            }
        });

        bMapyCZBase.setText("Mapy.cz Base");
        bMapyCZBase.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bMapyCZBaseActionPerformed(evt);
            }
        });

        bMapsGoogleCOM.setText("Google");
        bMapsGoogleCOM.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bMapsGoogleCOMActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pMap, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(bOSM)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(bBing)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(bMapquest)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(bMapyCZBase)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(bMapyCZBing)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(bMapyCZTourist)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(bMapsGoogleCOM)
                        .addGap(0, 94, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(bOSM)
                    .addComponent(bBing)
                    .addComponent(bMapquest)
                    .addComponent(bMapyCZTourist)
                    .addComponent(bMapyCZBing)
                    .addComponent(bMapyCZBase)
                    .addComponent(bMapsGoogleCOM))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(pMap, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private Coord readCoordinates(String input) {
        Coord coor = null;
        
        //52.2299;21.0628:180
        if (input.matches("[-.0-9]*;[-.0-9]*;[-.0-9]*")) {
            String[] s = input.trim().split(";");
            coor = new Coord(s[0], s[1], s[2]);
        }

        //52,2299;21,0628:180
        else if (input.matches("[-,0-9]*;[-,0-9]*;[-,0-9]*")) {
            input = input.replace(',', '.');
            String[] s = input.trim().split(";");
            coor = new Coord(s[0], s[1], s[2]);
        }

        //52.2299;21.0628
        if (input.matches("[-.0-9]*;[-.0-9]*")) {
            String[] s = input.trim().split(";");
            coor = new Coord(s[0], s[1]);
        }

        //52,2299;21,0628
        else if (input.matches("[-,0-9]*;[-,0-9]*")) {
            input = input.replace(',', '.');
            String[] s = input.trim().split(";");
            coor = new Coord(s[0], s[1]);
        }

        //www.openstreetmap.org/?lat=52.4075&lon=16.9315&zoom=13&layers=M
        else if (input.matches(".*lat=[-.0-9]*&lon=[-.0-9]*.*")) {
            String[] s = input.split("[#?&]");
            //JOptionPane.showMessageDialog(bSetCoor, s[1] + "----" + s[2]);
            String lat = "0", lon = "0";
            for (String i : s) {
                if (i.contains("lat")) {
                    lat = i.substring(4);
                }
                if (i.contains("lon")) {
                    lon = i.substring(4);
                }
            }
        }

        //{{Coord|52|14|5.15|N|21|7|51.91|E|region:PL}}
        else if (input.matches(".*[Kk]oordynaty|.*") || input.matches(".*[Cc]oord|.*")) {
            String[] s = input.split("\\|");
            if (s.length > 8) {    //długi
                String lat[] = { s[1], s[2], s[3] };
                String lon[] = { s[5], s[6], s[7] };
                coor = new Coord(lat, s[4], lon, s[8]);
            }
        }

        return coor;
    }

    private void bSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bSaveActionPerformed
        Data.settings.coor = getMapPosition();
        Data.settings.coorZoom = getMapZoom();

        String input = tCoor.getText();
        if (!input.isEmpty()) {
            Coord coor = readCoordinates(input);

            //push
            if (coor != null) {
                if (multiEdit) {
                    Data.fFileEdit.coor = coor;
                    Data.fFileEdit.tCoor.setText(coor.getDMSformated());
                    Data.fFileEdit.fCoord = null;
                } else {
                    Data.getFiles().get(number).coor = coor;
                    Data.getFiles().get(number).setCoordinates(coor);
                    Data.getFiles().get(number).fCoord = null;
                }
                dispose();
            } else {
                JOptionPane.showMessageDialog(rootPane, bundle.getString("coord-unknown"),
                    bundle.getString("file-coor"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_bSaveActionPerformed

    private void pMapMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_pMapMouseReleased
        // unused
        //JOptionPane.showMessageDialog(rootPane, string);
    }//GEN-LAST:event_pMapMouseReleased

    private void bOSMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bOSMActionPerformed
        DefaultTileFactory tileFactory = new DefaultTileFactory(new OSMTileFactoryInfo());
        mapViewer.setTileFactory(tileFactory);
    }//GEN-LAST:event_bOSMActionPerformed

    private void bBingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bBingActionPerformed
        mapViewer.setTileFactory(new DefaultTileFactory(new VirtualEarthTileFactoryInfo(VirtualEarthTileFactoryInfo.HYBRID)));
    }//GEN-LAST:event_bBingActionPerformed

    private void bMapquestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bMapquestActionPerformed
        TileFactoryInfo info = new TileFactoryInfo(1,17,19,
                    256, true, true,
                    "http://otile1.mqcdn.com/tiles/1.0.0/map",
                    "x","y","z") {
                @Override
                public String getTileUrl(int x, int y, int zoom) {
                    return this.baseURL +"/"+(19-zoom)+"/"+x+"/"+y+".jpg";
                }
        };
        mapViewer.setTileFactory(new DefaultTileFactory(info));
    }//GEN-LAST:event_bMapquestActionPerformed

    private void bMapyCZTouristActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bMapyCZTouristActionPerformed
        DefaultTileFactory tileFactory = new DefaultTileFactory(new MapyCZTileFactoryInfo("turist-m"));
        mapViewer.setTileFactory(tileFactory);
    }//GEN-LAST:event_bMapyCZTouristActionPerformed

    private void bMapyCZBingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bMapyCZBingActionPerformed
        DefaultTileFactory tileFactory = new DefaultTileFactory(new MapyCZTileFactoryInfo("bing"));
        mapViewer.setTileFactory(tileFactory);
    }//GEN-LAST:event_bMapyCZBingActionPerformed

    private void bMapyCZBaseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bMapyCZBaseActionPerformed
        DefaultTileFactory tileFactory = new DefaultTileFactory(new MapyCZTileFactoryInfo("base-m"));
        mapViewer.setTileFactory(tileFactory);
    }//GEN-LAST:event_bMapyCZBaseActionPerformed

    private void bMapsGoogleCOMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bMapsGoogleCOMActionPerformed
        DefaultTileFactory tileFactory = new DefaultTileFactory(new GoogleMapsTileFactoryInfo(""));
        mapViewer.setTileFactory(tileFactory);
    }//GEN-LAST:event_bMapsGoogleCOMActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton bBing;
    private javax.swing.JButton bMapquest;
    private javax.swing.JButton bMapsGoogleCOM;
    private javax.swing.JButton bMapyCZBase;
    private javax.swing.JButton bMapyCZBing;
    private javax.swing.JButton bMapyCZTourist;
    private javax.swing.JButton bOSM;
    private javax.swing.JButton bSave;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel pMap;
    private javax.swing.JTextField tCoor;
    // End of variables declaration//GEN-END:variables

    ResourceBundle bundle = java.util.ResourceBundle.getBundle("cuploader.text.messages");
    KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
    Action escapeAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
            dispose();
        }
        static final long serialVersionUID = -2856674781867817746L;
    };
    static final long serialVersionUID = 7619939933241725736L;
}
