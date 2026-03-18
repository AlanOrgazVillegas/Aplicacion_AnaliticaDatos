package analizadordataset;

import analizadordataset.modelo.Dataset;
import analizadordataset.modelo.Resultado;
import analizadordataset.controlador.ControladorTecnicas;
import analizadordataset.modelo.ResultadoClasificacion;
import analizadordataset.controlador.ControladorClasificacion;
import analizadordataset.metricas.Gower;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

public class PantallaPrincipal extends javax.swing.JFrame {

    private Dataset dataset;
    private ControladorTecnicas controlador;
    private ControladorClasificacion controladorClasif;
    
    public PantallaPrincipal() {
    initComponents();
    controlador = new ControladorTecnicas();
    controladorClasif = new ControladorClasificacion();
    buttonGroup1 = new javax.swing.ButtonGroup();
    buttonGroup1.add(radioRelief);
    buttonGroup1.add(CorrelationScore_radio);
    buttonGroup1.add(GreedyStepwise_radio);
    buttonGroup1.add(InformationGain);
    // Establecer action commands
    radioRelief.setActionCommand("RELIEF");
    CorrelationScore_radio.setActionCommand("CORRELATION");
    GreedyStepwise_radio.setActionCommand("GREEDY");
    InformationGain.setActionCommand("INFOGAIN");
    
    // Segundo grupo de botones para supervisada
    buttonGroup2 = new javax.swing.ButtonGroup();
    buttonGroup2.add(NaiveBayes_radio);
    buttonGroup2.add(J48_radio);
    buttonGroup2.add(KNN_radio);
    buttonGroup2.add(Reg_Log_radio);
    //action commands para supervisada
    NaiveBayes_radio.setActionCommand("NAIVEBAYES");
    J48_radio.setActionCommand("J48");
    KNN_radio.setActionCommand("KNN");
    Reg_Log_radio.setActionCommand("REGRESION_LOGISTICA");
    
}
    
    private void buscarDataset() {
        JFileChooser selector = new JFileChooser();
        FileNameExtensionFilter filtro = new FileNameExtensionFilter("Archivos CSV", "csv");
        selector.setFileFilter(filtro);
        
        int resultado = selector.showOpenDialog(this);
        
        if (resultado == JFileChooser.APPROVE_OPTION) {

            File archivo = selector.getSelectedFile();
            nombreDatasetLbl.setText(archivo.getName());

            try {
                dataset = new Dataset();
                dataset.cargar(archivo.getAbsolutePath());

                infoLbl.setText("Instancias: "
                        + dataset.getNumInstancias()
                        + " | Atributos: "
                        + dataset.getNumAtributos());
                
// --- AQUÍ EMPIEZA LO NUEVO PARA LA LISTA DE ATRIBUTOS ---
            javax.swing.DefaultListModel<String> modeloLista = new javax.swing.DefaultListModel<>();
            String[] nombresReales = dataset.getNombresColumnas(); // Usamos tu método
            
            for (int i = 0; i < dataset.getNumAtributos(); i++) {
                modeloLista.addElement(i + " - " + nombresReales[i]); 
            }
            listaAtributos.setModel(modeloLista);
            // --- AQUÍ TERMINA LO NUEVO ---

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Error al cargar dataset:\n" + e.getMessage());
            }
        }
    }

    private void ejecutarTecnica() {
    
    if (dataset == null) {
        JOptionPane.showMessageDialog(this,"Primero debes cargar un dataset.","Error",JOptionPane.ERROR_MESSAGE );
        return;
    }

    // Encontrar qué botón está seleccionado por su texto
    String tecnicaSeleccionada = "";
    
    if (radioRelief.isSelected()) {
        tecnicaSeleccionada = "RELIEF";
    } else if (CorrelationScore_radio.isSelected()) {
        tecnicaSeleccionada = "CORRELATION"; // Cambiado de FISHER a CORRELATION
    } else if (GreedyStepwise_radio.isSelected()) {
        tecnicaSeleccionada = "GREEDY";
    } else if (InformationGain.isSelected()) {
        tecnicaSeleccionada = "INFOGAIN";
    } else {
        JOptionPane.showMessageDialog(this,"Selecciona una técnica.","Información",JOptionPane.WARNING_MESSAGE);
        return;
    }
    
    System.out.println("Técnica seleccionada: " + tecnicaSeleccionada);
    
    // Validación para Correlation Score
    if (tecnicaSeleccionada.equals("CORRELATION")) { // Cambiado de FISHER a CORRELATION
        int claseIndex = dataset.getNumAtributos() - 1;
        if (dataset.getTipoColumna(claseIndex) != 'N') { // Cambiado de 'C' a 'N'
            JOptionPane.showMessageDialog(this,
                "Correlation Score requiere que la clase (última columna) sea numérica.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
    }
    
    if (tecnicaSeleccionada.equals("INFOGAIN")) {
            int claseIndex = dataset.getNumAtributos() - 1;
            if (dataset.getTipoColumna(claseIndex) != 'C') {
                JOptionPane.showMessageDialog(this,
                    "Information Gain requiere que la clase (última columna) sea categórica.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

    
    Resultado resultado = controlador.ejecutarTecnica(tecnicaSeleccionada, dataset);

    if (resultado != null) {
        txtAreaResultados.setText(resultado.generarReporte());
    } else {
        txtAreaResultados.setText("Error al ejecutar la técnica.");
    }
}
    
    private void ejecutarClasificacion() {
    
    if (dataset == null) {
        JOptionPane.showMessageDialog(this, 
            "Primero debes cargar un dataset.", 
            "Error", 
            JOptionPane.ERROR_MESSAGE);
        return;
    }

    // Determinar qué clasificador está seleccionado
    String clasificadorSeleccionado = "";
    
    if (NaiveBayes_radio.isSelected()) {
        clasificadorSeleccionado = "NAIVEBAYES";
    } else if (J48_radio.isSelected()) {
        clasificadorSeleccionado = "J48";
    } else if (KNN_radio.isSelected()) {
        clasificadorSeleccionado = "KNN";
    } else if (Reg_Log_radio.isSelected()) {
    clasificadorSeleccionado = "REGRESIONLOGISTICA";
    } else {
        JOptionPane.showMessageDialog(this, 
            "Selecciona un clasificador.", 
            "Información", 
            JOptionPane.WARNING_MESSAGE);
        return;
    }
    
    System.out.println("Clasificador seleccionado: " + clasificadorSeleccionado);
    
    // Validar que la clase sea categórica (todos los clasificadores la requieren)
    int claseIndex = dataset.getNumAtributos() - 1;
    if (dataset.getTipoColumna(claseIndex) != 'C') {
        JOptionPane.showMessageDialog(this,
            "Los clasificadores requieren que la clase (última columna) sea categórica.",
            "Error",
            JOptionPane.ERROR_MESSAGE);
        return;
    }
    
    try {
        ResultadoClasificacion resultado = controladorClasif.ejecutarClasificador(clasificadorSeleccionado, dataset);
        
        if (resultado != null) {
            txtAreaResultados1.setText(resultado.generarReporte());
        } else {
            txtAreaResultados1.setText("Error: El clasificador no está implementado.");
        }
    } catch (Exception e) {
        e.printStackTrace();
        txtAreaResultados1.setText("ERROR: " + e.getMessage());
        JOptionPane.showMessageDialog(this,
            "Error al ejecutar: " + e.getMessage(),
            "Error",
            JOptionPane.ERROR_MESSAGE);
    }
}
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();
        buttonGroup3 = new javax.swing.ButtonGroup();
        seleccionDatasetLbl = new javax.swing.JLabel();
        nombreDatasetLbl = new javax.swing.JTextField();
        buscarBtn = new javax.swing.JButton();
        tituloLbl = new javax.swing.JLabel();
        infoLbl = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        PanelPestañas = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        radioRelief = new javax.swing.JRadioButton();
        CorrelationScore_radio = new javax.swing.JRadioButton();
        GreedyStepwise_radio = new javax.swing.JRadioButton();
        InformationGain = new javax.swing.JRadioButton();
        btnIniciar = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtAreaResultados = new javax.swing.JTextArea();
        jScrollPane4 = new javax.swing.JScrollPane();
        listaAtributos = new javax.swing.JList<>();
        btnEliminarAtributos = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        NaiveBayes_radio = new javax.swing.JRadioButton();
        J48_radio = new javax.swing.JRadioButton();
        KNN_radio = new javax.swing.JRadioButton();
        Reg_Log_radio = new javax.swing.JRadioButton();
        btnIniciar2 = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        txtAreaResultados1 = new javax.swing.JTextArea();
        jPanel3 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        radioGowerSimilitud = new javax.swing.JRadioButton();
        radioGowerDistancia = new javax.swing.JRadioButton();
        btnIniciarGower = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        txtResultadosGower = new javax.swing.JTextArea();
        jPanel5 = new javax.swing.JPanel();
        jTabbedPane1 = new javax.swing.JTabbedPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        seleccionDatasetLbl.setText("Selecciona un dataset:");

        nombreDatasetLbl.setText("Nombre del dataset");

        buscarBtn.setText("Buscar");
        buscarBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buscarBtnActionPerformed(evt);
            }
        });

        tituloLbl.setText("Información del dataset:");

        infoLbl.setText("Info...");

        PanelPestañas.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        radioRelief.setText("Relief");
        radioRelief.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioReliefActionPerformed(evt);
            }
        });

        CorrelationScore_radio.setText("CorrelationScore");
        CorrelationScore_radio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CorrelationScore_radioActionPerformed(evt);
            }
        });

        GreedyStepwise_radio.setText("GreedyStepwise");
        GreedyStepwise_radio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GreedyStepwise_radioActionPerformed(evt);
            }
        });

        InformationGain.setText("Information Gain");

        btnIniciar.setText("Iniciar");
        btnIniciar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnIniciarActionPerformed(evt);
            }
        });

        txtAreaResultados.setColumns(20);
        txtAreaResultados.setRows(5);
        txtAreaResultados.addAncestorListener(new javax.swing.event.AncestorListener() {
            public void ancestorAdded(javax.swing.event.AncestorEvent evt) {
                txtAreaResultadosAncestorAdded(evt);
            }
            public void ancestorMoved(javax.swing.event.AncestorEvent evt) {
            }
            public void ancestorRemoved(javax.swing.event.AncestorEvent evt) {
            }
        });
        jScrollPane1.setViewportView(txtAreaResultados);

        listaAtributos.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane4.setViewportView(listaAtributos);

        btnEliminarAtributos.setText("Eliminar");
        btnEliminarAtributos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEliminarAtributosActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 611, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(radioRelief)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(CorrelationScore_radio)
                        .addGap(18, 18, 18)
                        .addComponent(GreedyStepwise_radio)
                        .addGap(21, 21, 21)
                        .addComponent(InformationGain)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(btnIniciar)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane4)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(btnEliminarAtributos)
                                .addGap(0, 184, Short.MAX_VALUE)))
                        .addGap(23, 23, 23))))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(radioRelief)
                            .addComponent(CorrelationScore_radio)
                            .addComponent(GreedyStepwise_radio)
                            .addComponent(InformationGain))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 233, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(19, 19, 19)
                        .addComponent(btnIniciar)
                        .addGap(18, 18, 18)
                        .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnEliminarAtributos)))
                .addContainerGap(25, Short.MAX_VALUE))
        );

        PanelPestañas.addTab("Preprocesamiento", jPanel1);

        NaiveBayes_radio.setText("NaiveBayes");
        NaiveBayes_radio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                NaiveBayes_radioActionPerformed(evt);
            }
        });

        J48_radio.setText("J48");
        J48_radio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                J48_radioActionPerformed(evt);
            }
        });

        KNN_radio.setText("KNN");
        KNN_radio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                KNN_radioActionPerformed(evt);
            }
        });

        Reg_Log_radio.setText("Regresion Logistica");
        Reg_Log_radio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Reg_Log_radioActionPerformed(evt);
            }
        });

        btnIniciar2.setText("Iniciar");
        btnIniciar2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnIniciar2ActionPerformed(evt);
            }
        });

        txtAreaResultados1.setColumns(20);
        txtAreaResultados1.setRows(5);
        txtAreaResultados1.addAncestorListener(new javax.swing.event.AncestorListener() {
            public void ancestorAdded(javax.swing.event.AncestorEvent evt) {
                txtAreaResultados1AncestorAdded(evt);
            }
            public void ancestorMoved(javax.swing.event.AncestorEvent evt) {
            }
            public void ancestorRemoved(javax.swing.event.AncestorEvent evt) {
            }
        });
        jScrollPane2.setViewportView(txtAreaResultados1);

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(NaiveBayes_radio)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(J48_radio)
                .addGap(28, 28, 28)
                .addComponent(KNN_radio)
                .addGap(18, 18, 18)
                .addComponent(Reg_Log_radio)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(0, 6, Short.MAX_VALUE)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 611, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, Short.MAX_VALUE)
                .addComponent(btnIniciar2)
                .addGap(26, 26, 26))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(NaiveBayes_radio)
                    .addComponent(J48_radio)
                    .addComponent(KNN_radio)
                    .addComponent(Reg_Log_radio))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnIniciar2)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 233, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(25, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        PanelPestañas.addTab("Clasificación supervisada", jPanel2);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 917, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 297, Short.MAX_VALUE)
        );

        PanelPestañas.addTab("Clasificación no supervisada", jPanel3);

        radioGowerSimilitud.setText("Similitud");

        radioGowerDistancia.setText("Distancia");

        btnIniciarGower.setText("Iniciar");
        btnIniciarGower.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnIniciarGowerActionPerformed(evt);
            }
        });

        txtResultadosGower.setColumns(20);
        txtResultadosGower.setRows(5);
        jScrollPane3.setViewportView(txtResultadosGower);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 641, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 192, Short.MAX_VALUE)
                        .addComponent(btnIniciarGower))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(radioGowerSimilitud)
                        .addGap(18, 18, 18)
                        .addComponent(radioGowerDistancia)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radioGowerSimilitud)
                    .addComponent(radioGowerDistancia))
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 258, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addComponent(btnIniciarGower)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );

        PanelPestañas.addTab("Gower", jPanel4);

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 917, Short.MAX_VALUE)
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 297, Short.MAX_VALUE)
        );

        PanelPestañas.addTab("Resultados", jPanel5);
        PanelPestañas.addTab("Predicción", jTabbedPane1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(seleccionDatasetLbl, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(tituloLbl, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(nombreDatasetLbl, javax.swing.GroupLayout.PREFERRED_SIZE, 257, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(buscarBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addComponent(infoLbl, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(16, 16, 16)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 670, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addComponent(PanelPestañas))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(seleccionDatasetLbl)
                    .addComponent(nombreDatasetLbl, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buscarBtn))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tituloLbl)
                    .addComponent(infoLbl))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(PanelPestañas)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buscarBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buscarBtnActionPerformed
        buscarDataset();
    }//GEN-LAST:event_buscarBtnActionPerformed
        
    private void btnIniciarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnIniciarActionPerformed
        ejecutarTecnica();
    }//GEN-LAST:event_btnIniciarActionPerformed

    private void radioReliefActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioReliefActionPerformed

    }//GEN-LAST:event_radioReliefActionPerformed

    private void txtAreaResultadosAncestorAdded(javax.swing.event.AncestorEvent evt) {//GEN-FIRST:event_txtAreaResultadosAncestorAdded
        // TODO add your handling code here:
    }//GEN-LAST:event_txtAreaResultadosAncestorAdded

    private void CorrelationScore_radioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CorrelationScore_radioActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_CorrelationScore_radioActionPerformed

    private void NaiveBayes_radioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_NaiveBayes_radioActionPerformed
    System.out.println("NaiveBayes seleccionado");
    }//GEN-LAST:event_NaiveBayes_radioActionPerformed

    private void J48_radioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_J48_radioActionPerformed
    System.out.println("J48 seleccionado");
    }//GEN-LAST:event_J48_radioActionPerformed

    private void btnIniciar2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnIniciar2ActionPerformed
         ejecutarClasificacion();
    }//GEN-LAST:event_btnIniciar2ActionPerformed

    private void txtAreaResultados1AncestorAdded(javax.swing.event.AncestorEvent evt) {//GEN-FIRST:event_txtAreaResultados1AncestorAdded
        // TODO add your handling code here:
    }//GEN-LAST:event_txtAreaResultados1AncestorAdded

    private void GreedyStepwise_radioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GreedyStepwise_radioActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_GreedyStepwise_radioActionPerformed

    private void KNN_radioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_KNN_radioActionPerformed
        System.out.println("KNN seleccionado");
    }//GEN-LAST:event_KNN_radioActionPerformed

    private void Reg_Log_radioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Reg_Log_radioActionPerformed
        System.out.println("Regresion Logistica seleccionada");
    }//GEN-LAST:event_Reg_Log_radioActionPerformed

    private void btnIniciarGowerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnIniciarGowerActionPerformed
// 1. Validar que haya un dataset cargado
    if (dataset == null) {
        JOptionPane.showMessageDialog(this, "Primero debes cargar un dataset.", "Error", JOptionPane.ERROR_MESSAGE);
        return;
    }

    // 2. Saber qué eligió el usuario (usa los nombres de variables que le pusiste a tus radios)
    boolean esSimilitud = radioGowerSimilitud.isSelected();
    boolean esDistancia = radioGowerDistancia.isSelected();

    if (!esSimilitud && !esDistancia) {
        JOptionPane.showMessageDialog(this, "Selecciona Similitud o Distancia.", "Aviso", JOptionPane.WARNING_MESSAGE);
        return;
    }

    // 3. Preparar la pantalla de resultados
    txtResultadosGower.setText("--- ANÁLISIS DE GOWER ---\n");
    txtResultadosGower.append("Dataset cargado. Total de instancias: " + dataset.getNumInstancias() + "\n");
    
    if (esSimilitud) {
        txtResultadosGower.append("Calculando SIMILITUD (1.0 = Idénticos, 0.0 = Diferentes)\n");
    } else {
        txtResultadosGower.append("Calculando DISTANCIA (0.0 = Idénticos, 1.0 = Muy lejanos)\n");
    }
    
    txtResultadosGower.append("Comparando la Fila 0 con el resto...\n");
    txtResultadosGower.append("--------------------------------------------------\n\n");

    try {
        // 4. Instanciar nuestra clase lógica
        Gower gower = new Gower(dataset);
        int numInstancias = dataset.getNumInstancias();

        // 5. Ciclo para comparar la fila 0 contra todas las demás (empezamos en 1)
        for (int i = 1; i < numInstancias; i++) {
            double resultado;
            
            if (esSimilitud) {
                resultado = gower.calcularSimilitud(0, i);
                // String.format nos ayuda a que solo salgan 4 decimales para que se vea limpio
                txtResultadosGower.append(String.format("Fila 0 vs Fila %d -> %.4f\n", i, resultado));
            } else {
                resultado = gower.calcularDistancia(0, i);
                txtResultadosGower.append(String.format("Fila 0 vs Fila %d -> %.4f\n", i, resultado));
            }
        }
        
        txtResultadosGower.append("\n¡Cálculo finalizado exitosamente!");
        
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error al calcular Gower: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
    }//GEN-LAST:event_btnIniciarGowerActionPerformed

    private void btnEliminarAtributosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEliminarAtributosActionPerformed
if (dataset == null) {
        JOptionPane.showMessageDialog(this, "Primero carga un dataset.", "Error", JOptionPane.ERROR_MESSAGE);
        return;
    }

    // 1. Obtener qué seleccionó el usuario en la lista
    int[] indicesSeleccionados = listaAtributos.getSelectedIndices();
    if (indicesSeleccionados.length == 0) {
        JOptionPane.showMessageDialog(this, "Selecciona al menos una columna de la lista para eliminar.", "Aviso", JOptionPane.WARNING_MESSAGE);
        return;
    }

    try {
        // 2. Llamar al nuevo método mágico que recorta el dataset
        dataset.eliminarColumnas(indicesSeleccionados);

        // 3. Actualizar la etiqueta que muestra cuántos atributos quedan
        infoLbl.setText("Instancias: " + dataset.getNumInstancias() + " | Atributos: " + dataset.getNumAtributos());

        // 4. Refrescar la lista visual para que desaparezcan las columnas borradas
        javax.swing.DefaultListModel<String> modeloLista = new javax.swing.DefaultListModel<>();
        String[] nombresActualizados = dataset.getNombresColumnas();
        
        for (int i = 0; i < dataset.getNumAtributos(); i++) {
            modeloLista.addElement(i + " - " + nombresActualizados[i]);
        }
        listaAtributos.setModel(modeloLista);

        JOptionPane.showMessageDialog(this, "Columnas eliminadas correctamente. El dataset ha sido actualizado.", "Éxito", JOptionPane.INFORMATION_MESSAGE);

    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error al eliminar: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
    }//GEN-LAST:event_btnEliminarAtributosActionPerformed

    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(PantallaPrincipal.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(PantallaPrincipal.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(PantallaPrincipal.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(PantallaPrincipal.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new PantallaPrincipal().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JRadioButton CorrelationScore_radio;
    private javax.swing.JRadioButton GreedyStepwise_radio;
    private javax.swing.JRadioButton InformationGain;
    private javax.swing.JRadioButton J48_radio;
    private javax.swing.JRadioButton KNN_radio;
    private javax.swing.JRadioButton NaiveBayes_radio;
    private javax.swing.JTabbedPane PanelPestañas;
    private javax.swing.JRadioButton Reg_Log_radio;
    private javax.swing.JButton btnEliminarAtributos;
    private javax.swing.JButton btnIniciar;
    private javax.swing.JButton btnIniciar2;
    private javax.swing.JButton btnIniciarGower;
    private javax.swing.JButton buscarBtn;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.ButtonGroup buttonGroup3;
    private javax.swing.JLabel infoLbl;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JList<String> listaAtributos;
    private javax.swing.JTextField nombreDatasetLbl;
    private javax.swing.JRadioButton radioGowerDistancia;
    private javax.swing.JRadioButton radioGowerSimilitud;
    private javax.swing.JRadioButton radioRelief;
    private javax.swing.JLabel seleccionDatasetLbl;
    private javax.swing.JLabel tituloLbl;
    private javax.swing.JTextArea txtAreaResultados;
    private javax.swing.JTextArea txtAreaResultados1;
    private javax.swing.JTextArea txtResultadosGower;
    // End of variables declaration//GEN-END:variables
}