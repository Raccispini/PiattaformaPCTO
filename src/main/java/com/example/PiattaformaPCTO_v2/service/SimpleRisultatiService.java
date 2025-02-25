package com.example.PiattaformaPCTO_v2.service;

import com.example.PiattaformaPCTO_v2.collection.*;
import com.example.PiattaformaPCTO_v2.dto.ActivityViewDTOPair;
import com.example.PiattaformaPCTO_v2.repository.AttivitaRepository;
import com.example.PiattaformaPCTO_v2.repository.ScuolaRepository;
import com.example.PiattaformaPCTO_v2.repository.UniversitarioRepository;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

@Service
public class SimpleRisultatiService implements RisultatiService {


    @Autowired
    private AttivitaRepository attivitaRepository;

    @Autowired
    private UniversitarioRepository universitarioRepository;
    @Autowired
    private ScuolaRepository scuolaRepository;
    private AttivitaService attivitaService;

    @Override
    public void crea() {
        //Ottengo la lista di tutte le attività, da migliorare filtrando con l'anno accademico di svolgimento
        List<Attivita> attivita = attivitaRepository.findAll();

        //Creo una lista vuota per contenere tutti i risultati
        List<Risultati> r = new ArrayList<>();

        //Scorro la lista delle attività presenti, da miglirare anche con un for each
        for (int i=0;i<attivita.size()-1;i++){

            //ottengo il nome dell'attività
            String nome= attivita.get(i).getNome();
            //System.out.println(nome);

            // dalla singola attività ottengo la lista degli studenti partecipanti
            List<Studente> stud = attivita.get(i).getStudPartecipanti();

            // scorro tutti gli studenti
            for (int x = 0; x < stud.size(); x++) {

                //dal singolo studente ottengo la scuola che frequenta
                Scuola s = stud.get(x).getScuola();
                // Prendo l'id, si puo collassare in una dichiarazione sola
                String scuola = s.getIdScuola();

                //Utilizzo il metodo per ottenere l'indice in cui inserire la scuola
                int index=this.scuoleHelperd(r,scuola);
                // nel caso la scuola sia presente
                if (index != -1) {

                    //Utilizzo il metodo per ottenere l'indice in cui inserire l'attivita
                    int a = this.attivitaHelper(r.get(index).getAttivita(), nome);
                    //nel caso in cui l'attivta sia gia presente aggiungo la presenza di quello studente
                    if (a != -1) {
                        r.get(index).getAttivita().get(a).getPartecipanti().add(stud.get(x));
                    } else {
                        //mentre se non è presente creo la presenza
                        Presenza p = new Presenza(nome);
                        p.getPartecipanti().add(stud.get(x));
                        r.get(index).getAttivita().add(p);
                    }
                } else {
                    //nel caso in cui non sia presente la scuola creo una nuova presenza e un risultato per la scuola
                    Presenza p = new Presenza(nome);
                    Risultati res = new Risultati("21/22", s);
                    p.getPartecipanti().add(stud.get(x));
                    res.getAttivita().add(p);
                    r.add(res);
                }
            }
        }
        //Scorro di nuovo la lista dei risultati per controllare il numero di studenti che si sono iscritti all'università
        for (int y=0;y<r.size();y++){
            for(int b=0;b<r.get(y).getAttivita().size();b++){
                for(int c=0;c<r.get(y).getAttivita().get(b).getPartecipanti().size();c++){
                    Studente studi=r.get(y).getAttivita().get(b).getPartecipanti().get(c);
                    String nome= studi.getNome().toUpperCase();
                    String cognome= studi.getCognome().toUpperCase();
                    String citta= studi.getScuola().getCitta().toUpperCase();
                    Universitario uni =universitarioRepository.findByNomeAndCognomeAndComuneScuola(nome,cognome,citta);
                    if(uni!=null){
                        //System.out.println(uni.getCognome()+"   "+uni.getScuolaProv());
                        r.get(y).getAttivita().get(b).nuovoIscritto(studi);
                        if (this.studenteHelper(r, studi) == 0) {
                            r.get(y).nuovoIscritto(studi);
                        }

                    }
                }
            }
        }
        risultatiInf(attivita,r);
        /*or (Risultati risultati: r){
            for (Studente s : risultati.getIscritti()){
                System.out.println(s.toString());
            }
        }*/
        //this.creaRisultato(r);
        //this.createexcl(r);
        this.createword(r);
    }

    @Override
    public void createStudentsFromActivities() {
        Map<Attivita, List<ActivityViewDTOPair>> result = new HashMap<>();
        List<Attivita> activities = this.attivitaRepository.findAll();
        activities.forEach(a -> result.put(a, attivitaService.findStudentsFromActivity(a.getNome())));
        XWPFDocument xwpfDocument = new XWPFDocument();
        String filepath = "src/main/resources/StudentsFromActivities.docx";
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(filepath);
            result.entrySet().forEach(e -> {
                XWPFParagraph paragraph = xwpfDocument.createParagraph();
                XWPFRun run = paragraph.createRun();
                Attivita activity = e.getKey();
                run.setText(String.format("Nome: %s | Anno: %s", activity.getNome(), activity.getAnnoAcc()));
                run.addBreak();
                long universityStudentsCount = e.getValue().stream().count();
                run.setText(String.format("Totale iscritti dall'attività: %d", universityStudentsCount));
                run.addBreak();
                if (universityStudentsCount > 0){run.setText("Elenco universitari:");
                    run.addBreak();
                    for (ActivityViewDTOPair p : e.getValue()) {
                        run.setText(String.format("%s (%s), %s %s, %s | %s | %s, %s, %s",
                                p.universityStudent().getMatricola(),
                                p.universityStudent().getAnnoImm(),
                                p.universityStudent().getNome(),
                                p.universityStudent().getCognome(),
                                p.highSchool().getNome(),
                                p.highSchool().getTipo(),
                                p.highSchool().getCitta(),
                                p.highSchool().getProvincia(),
                                p.highSchool().getRegione()));
                        run.addBreak();
                    }
                }
                run.addBreak();
            });
            xwpfDocument.write(fileOutputStream);
            xwpfDocument.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public void risultatiInf(List<Attivita> attivita, List<Risultati> r) {
        Attivita att = attivita.get(4);
        String nome = att.getNome();
        for (Studente s : att.getStudPartecipanti()){
            Universitario uni = universitarioRepository.findByNomeAndCognome(s.getNome().toUpperCase(),s.getCognome().toUpperCase());
            if(uni!= null){
                System.out.println(s.toString());
                Scuola scuola = findScuola(uni.getComuneScuola(), uni.getScuolaProv());
                s.setScuola(scuola);
                int index=this.scuoleHelperd(r,scuola.getIdScuola());
                if (index!=-1){
                    int a=this.attivitaHelper(r.get(index).getAttivita(),nome);
                    if (a!=-1){
                        System.out.println("Prima "+r.get(index).getAttivita().get(a).getIscritti().size());
                        r.get(index).getAttivita().get(a).getPartecipanti().add(s);
                        r.get(index).getAttivita().get(a).nuovoIscritto(s);
                        System.out.println("Dopo "+r.get(index).getAttivita().get(a).getIscritti().size());
                        if(this.studenteHelper(r,s)==0){
                            r.get(index).nuovoIscritto(s);
                        }
                    }else{
                        System.out.println(" "+r.get(index).getAttivita().get(0).getIscritti().size());
                        Presenza p = new Presenza(nome);
                        p.getPartecipanti().add(s);
                        p.nuovoIscritto(s);
                        r.get(index).getAttivita().add(p);
                        System.out.println("Dopo "+r.get(index).getAttivita().get(0).getIscritti().size());
                        if (this.studenteHelper(r,s)==0){
                            r.get(index).nuovoIscritto(s);
                        }
                    }
                }else{
                    Presenza p = new Presenza(nome);
                    Risultati res = new Risultati("21/22",scuola);
                    p.getPartecipanti().add(s);
                    p.nuovoIscritto(s);
                    res.getAttivita().add(p);
                    res.nuovoIscritto(s);
                    r.add(res);
                }
            }
        }

       /*System.out.println("entra");
        List<Attivita> atti =attivitaRepository.findAll();
        Attivita a = atti.get(4);
        for (Studente s :a.getStudPartecipanti()){
            Universitario uni = universitarioRepository.findByNomeAndCognome(s.getNome().toUpperCase(),s.getCognome().toUpperCase());
           if(uni!= null){
                System.out.println("uno c'e");
                System.out.println(uni.toString());
                Scuola scuola = findScuola(uni.getComuneScuola(), uni.getScuolaProv());
               System.out.println(scuola.toString());
            }
        }*/

    }



    private Scuola findScuola(String citta, String scuola){
        List<Scuola> scuole = scuolaRepository.getScuolaByCitta(citta);
        List<String> nomi = new ArrayList<>();
        for (Scuola s : scuole){
            nomi.add(s.getNome());
        }
        return  scuolaRepository.getScuolaByNome(findMostSimilarString(scuola,nomi));
    }



    public  String findMostSimilarString(String input, @org.jetbrains.annotations.NotNull List<String> strings) {
        String mostSimilarString = "";
        int minDistance = Integer.MAX_VALUE;
        for (String str : strings) {
            int distance = levenshteinDistance(input, str);
            if (distance < minDistance) {
                minDistance = distance;
                mostSimilarString = str;
            }
        }
        return mostSimilarString;
    }

    private  int levenshteinDistance(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();
        int[][] dp = new int[m+1][n+1];
        for (int i = 0; i <= m; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= n; j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i-1) == s2.charAt(j-1)) {
                    dp[i][j] = dp[i-1][j-1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i-1][j-1], Math.min(dp[i-1][j], dp[i][j-1]));
                }
            }
        }
        return dp[m][n];
    }
    /**
     * Metodo che controlla se una scuola è presente all'interno della lista dei risultati
     *
     * @param res la lista dei risultati
     * @param id  l'id della scuola
     * @return l'indice a cui si trova nel caso sia presente, -1 nel caso non sia presente
     */
    private int scuoleHelperd(List<Risultati> res, String id) {
        if (res.size() == 0) {
            return -1;
        } else {
            for (int i = 0; i < res.size(); i++) {
                if (res.get(i).getScuola().getIdScuola().equals(id)) {
                    return i;
                }
            }
        }
        return -1;
    }


    /**
     * Metodo che controlla se una scuola è presente all'interno della lista dei risultati
     *
     * @param pres
     * @param nome
     * @return
     */
    private int attivitaHelper(List<Presenza> pres, String nome) {
        for (int i = 0; i < pres.size(); i++) {
            if (Objects.equals(pres.get(i).getNomeAttivita(), nome)) {
                // System.out.println("sta qua"+i);
                return i;
            }
        }
        return -1;
    }


   /* private void createexcl(List<Risultati> r){
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Risultati PCTO 21-22");
        sheet.createRow(0);
        sheet.getRow(0).createCell(0).setCellValue("Scuola");
        sheet.getRow(0).createCell(1).setCellValue("Città");
        sheet.getRow(0).createCell(2).setCellValue("Regione");
        sheet.getRow(0).createCell(3).setCellValue("Iscritti tot.");
        for (int i=0;i<r.size();i++){
            sheet.createRow(i+1);
            sheet.getRow(i+1).createCell(0).setCellValue(r.get(i).getScuola().getNome());
            sheet.getRow(i+1).createCell(1).setCellValue(r.get(i).getScuola().getCitta());
            sheet.getRow(i+1).createCell(2).setCellValue(r.get(i).getScuola().getRegione());
            sheet.getRow(i+1).createCell(3).setCellValue(r.get(i).getIscritti().size());
            int index=4;
            int y=0;
            for (int x=0;x<r.get(i).getAttivita().size();x++){
                sheet.getRow(0).createCell(index).setCellValue("Attività");
                sheet.getRow(i+1).createCell(index).setCellValue(r.get(i).getAttivita().get(x).getNomeAttivita());
                index++;
                sheet.getRow(0).createCell(index).setCellValue("Anno Accademico Svolgimento");
                sheet.getRow(i+1).createCell(index).setCellValue("2021-2022");
                index++;
                sheet.getRow(0).createCell(index).setCellValue("Studenti partecipanti");
                sheet.getRow(i+1).createCell(index).setCellValue(r.get(i).getAttivita().get(x).getPartecipanti().size());
                index++;
                sheet.getRow(0).createCell(index).setCellValue("Iscritti ottenuti");
                sheet.getRow(i+1).createCell(index).setCellValue(r.get(i).getAttivita().get(x).getIscritti());
                index++;
                y=x;

            }
            int off=0;
            if(r.get(i).getIscritti().size()>0){
                for (int c=0;c<r.get(i).getAttivita().get(y).getPartecipanti().size();c++){
                    Studente studi=r.get(i).getAttivita().get(y).getPartecipanti().get(c);
                    String nome= studi.getNome().toUpperCase();
                    String cognome= studi.getCognome().toUpperCase();
                    String citta= studi.getScuola().getCitta().toUpperCase();
                    Universitario uni =universitarioRepository.findByNomeAndCognomeAndComuneScuola(nome,cognome,citta);

                    if(uni!=null){
                        sheet.getRow(0).createCell(12+off).setCellValue("Nome");
                        String stud= uni.getNome()+" "+uni.getCognome();
                        sheet.getRow(i+1).createCell(12+off).setCellValue(stud);
                        off++;
                    }
                }
            }
        }
        FileOutputStream file = null;
        try {
            file = new FileOutputStream("src/main/resources/Risultati3.xlsx");
            workbook.write(file);
            workbook.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }*/


    private void creaRisultato(List<Risultati> r) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Risultati PCTO 21-22");
        sheet.createRow(0);
        sheet.getRow(0).createCell(0).setCellValue("Scuola");
        sheet.getRow(0).createCell(1).setCellValue("Iscritti tot.");

        for (int i=0;i<r.size();i++){
            int index=2;
            sheet.createRow(i+1);
            sheet.getRow(i+1).createCell(0).setCellValue(r.get(i).getScuola().getNome());
            sheet.getRow(i+1).createCell(1).setCellValue(r.get(i).getIscritti().size());
            for (int x = 0;x<r.get(i).getAttivita().size();x++){
                sheet.getRow(i+1).createCell(index).setCellValue(r.get(i).getAttivita().get(x).getNomeAttivita());
                sheet.getRow(i+1).createCell(index+1).setCellValue(r.get(i).getAttivita().get(x).getIscritti().size());
                index+=2;
            }
        }
        FileOutputStream file = null;
        try {
            file = new FileOutputStream("src/main/resources/Risultati4.xlsx");
            workbook.write(file);
            workbook.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private int studenteHelper(List<Risultati> risultati,Studente studente){
        for (Risultati r : risultati){
            for (Studente s : r.getIscritti()){
                if (s.getNome().toUpperCase().equals(studente.getNome().toUpperCase())&&
                s.getCognome().toUpperCase().equals(studente.getCognome().toUpperCase())){
                    return -1;
                }
            }
        }
        return 0;
    }


    private void createword(List<Risultati> r) {
        XWPFDocument xwpfDocument = new XWPFDocument();
        XWPFDocument nonIscr = new XWPFDocument();
        String filepath="src/main/resources/Iscritti.docx";
        String file2 ="src/main/resources/NonIscritti.docx";
        try {
            FileOutputStream fileOutputStream =new FileOutputStream(filepath);
            FileOutputStream fileOutputNon = new FileOutputStream(file2);
            for(Risultati risultati: r){
                if(risultati.getIscritti().size()>0){
                    XWPFParagraph paragraph = xwpfDocument.createParagraph();
                    XWPFRun run = paragraph.createRun();
                    Scuola sc=risultati.getScuola();
                    run.setText(sc.getNome()+" "+ sc.getRegione()+" "+sc.getCitta());
                    run.addBreak();
                    run.setText(" Totale iscritti all'università: ");
                    run.setText(String.valueOf(risultati.getIscritti().size()));
                    run.addBreak();
                    for(Presenza p : risultati.getAttivita()){
                        run.setText(p.getNomeAttivita());
                        run.setText(" Partecipanti : ");
                        run.setText(String.valueOf(p.getPartecipanti().size()));
                        run.setText(" Iscritti: ");
                        run.setText(String.valueOf(p.getIscritti().size()));
                        run.addBreak();
                    }
                    run.setText("Elenco iscritti: ");
                    run.addBreak();
                    for (Studente s:risultati.getIscritti()){
                        run.setText(s.getNome()+" "+s.getCognome());
                        run.addBreak();
                    }
                    run.addBreak();
                }else{
                    XWPFParagraph paragraph2 = nonIscr.createParagraph();
                    XWPFRun run2 = paragraph2.createRun();
                    Scuola sc=risultati.getScuola();
                    run2.setText(sc.getNome()+" "+ sc.getRegione()+" "+sc.getCitta());
                    run2.addBreak();
                    for(Presenza p : risultati.getAttivita()){
                        run2.setText(p.getNomeAttivita());
                        run2.setText(" Partecipanti : ");
                        run2.setText(String.valueOf(p.getPartecipanti().size()));
                        run2.addBreak();
                        run2.addBreak();
                    }
                }
            }
            xwpfDocument.write(fileOutputStream);
            xwpfDocument.close();
            nonIscr.write(fileOutputNon);
            nonIscr.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }




}
