package com.example.demo;

import com.vaadin.event.dd.acceptcriteria.Not;
import com.vaadin.server.StreamResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.*;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;

@SpringUI
public class PageUI extends UI {
    VerticalLayout layout, summaryLayout;
    int days, hours, mins;
    TextField wysokoscDiety, odleglosc, sniadania, obiady, kolacje, zaliczka, cenaBiletu;
    RadioButtonGroup rodzajPojazdu;
    ArrayList<String> pojazdy, errors;
    Double kosztPrzejazdu, kosztDiet, sumaKosztow;
    DateTimeField dataWyjazdu, dataPowrotu;
    Label warning;
    @Override
    protected void init(VaadinRequest vaadinRequest)
    {
        layout = new VerticalLayout();
        errors = new ArrayList<>();
        summaryLayout = new VerticalLayout();
        setContent(layout);
        Label header = new Label("Diety");
        header.addStyleName("h1");
        layout.addComponent(header);
        dataWyjazdu = new DateTimeField();
        dataWyjazdu.setValue(LocalDateTime.now());
        dataWyjazdu.addValueChangeListener(event -> Notification.show("Value changed:", String.valueOf(event.getValue()), Notification.Type.TRAY_NOTIFICATION));
        dataWyjazdu.setCaption("Data i godzina wyjazdu");
        dataPowrotu = new DateTimeField();
        dataPowrotu.setValue(LocalDateTime.now());
        dataPowrotu.addValueChangeListener(event -> Notification.show("Value changed:", String.valueOf(event.getValue()), Notification.Type.TRAY_NOTIFICATION));
        dataPowrotu.setCaption("Data i godzina powrotu");
        layout.addComponents(dataWyjazdu, dataPowrotu);
        wysokoscDiety = new TextField("Wysokość diety za dobę podróży w zł");
        layout.addComponent(wysokoscDiety);
        Label koszt = new Label("Koszt zapewnionego bezpłatnego wyżywienia zmniejszający diety");
        koszt.addStyleName("h2");
        layout.addComponent(koszt);
        sniadania = new TextField("Liczba śniadań:");
        obiady = new TextField("Liczba obiadów:");
        kolacje = new TextField("Liczba kolacji:");
        layout.addComponents(sniadania, obiady, kolacje);
        Label zwrot = new Label("Zwrot kosztów przejazdu");
        zwrot.addStyleName("h1");
        layout.addComponent(zwrot);
        TextField srodekTransportu = new TextField("Rodzaj środka transportu");
        cenaBiletu = new TextField("Cena biletu za przejazd w zł");
        layout.addComponents(srodekTransportu, cenaBiletu, new Label("Przejazd niebędącym własnością pracodawcy (liczba faktycznie przejachanych kilometrów):"));
        pojazdy = new ArrayList<>();
        pojazdy.addAll(Arrays.asList("Samochód osobowy z silnikiem o poj. do 900 cm3", "Samochód osobowy z silnikiem o poj. od 900 cm3", "Motocykl", "Motorower"));
        rodzajPojazdu = new RadioButtonGroup("Rodzaj pojazdu:",pojazdy);
        odleglosc = new TextField("Odległość w kilometrach:");
        zaliczka = new TextField("Pobrana zaliczka w zł:");
        warning = new Label();
        layout.addComponents(rodzajPojazdu, odleglosc, zaliczka);
        Label digw = new Label();
        Label digp = new Label();
        Label cp = new Label();
        Label wd = new Label();
        Label przejazd = new Label();
        Label kosztyPrzejazdow = new Label();
        Label kosztBiletow = new Label();
        Label suma = new Label();
        Label pobranaZaliczka = new Label();
        Label doWyplaty = new Label();
        Button oblicz = new Button("Oblicz");
        oblicz.addClickListener(clickEvent -> {
            if (!validate()) {
                for(String errory : errors)
                {
                    Notification.show("Error", errory, Notification.TYPE_TRAY_NOTIFICATION);
                };
                return;
            }
            digw.setValue("Data i godzina wyjazdu: "+ getNiceDate(dataWyjazdu.getValue()));
            digp.setValue("Data i godzina powrotu: "+ getNiceDate(dataPowrotu.getValue()));
            cp.setValue("Czas podróży: "+getDateDifference());
            wd.setValue("Wysokość diet: "+ getWysokoscDiet());
            przejazd.setValue(getOpisPrzejazdu());
            kosztyPrzejazdow.setValue(getKosztyPrzejzadow());
            kosztBiletow.setValue(getKosztBiletow());
            suma.setValue(getSuma());
            pobranaZaliczka.setValue(getPobranaZaliczka());
            doWyplaty.setValue(getDoWyplaty());
            setContent(summaryLayout);
        });
        layout.addComponents(oblicz, warning);
        Label wyniki = new Label("Wyniki");
        wyniki.addStyleName("h1");
        Label diety = new Label("Diety");
        diety.addStyleName("h2");
        Label koszty = new Label ("Zwrot kosztów przejazdu");
        koszty.addStyleName("h2");
        Label wynik = new Label("Wynik");
        wynik.addStyleName("h2");
        summaryLayout.addComponents(wyniki, diety, digw, digp, cp, wd, koszty, przejazd, kosztyPrzejazdow, kosztBiletow, wynik, suma, pobranaZaliczka, doWyplaty);
        Button powrot = new Button("Powrot");
        powrot.addClickListener(clickEvent -> {
           setContent(layout);
        });
        summaryLayout.addComponent(powrot);
    }

    private boolean validate()
    {
        daysHoursMins();
        errors.clear();
        if (days<1 || (days==0 && hours<1))
            errors.add("Data powrotu musi byc pozniejsza nic data wyjazdu!");
        if (!validateTextField(wysokoscDiety))
            errors.add("Wysokosc diety musi byc liczbą!");
        if (!validateTextField(sniadania)||!validateTextField(obiady)||!validateTextField(kolacje))
            errors.add("Koszty posiłków powinny być liczbą!");
        if(!validateTextField(cenaBiletu))
            errors.add("Cena biletu powinna być liczbą!");
        if(!validateTextField(odleglosc))
            errors.add("Odległość przejazdu powinna być liczbą!");
        if(!validateTextField(zaliczka))
            errors.add("Wysokość zaliczki powinna być liczbą!");
        if (errors.size()>0)
            return false;
        return true;
    }

    private boolean validateTextField(TextField textField)
    {
        if(textField.getValue().matches("\\d+(\\.\\d+)?") || textField.getValue().equals(""))
            return true;
        return false;
    }

    private String getKosztBiletow() {
        if(cenaBiletu.getValue().equals("")){
            return "Koszt biletów: 0 zł";
        }
        return "Koszt biletów: "+cenaBiletu.getValue()+" zł";
    }

    private String getDoWyplaty() {
        Double wartoscZaliczki=0.0;
        if (!zaliczka.getValue().equals(""))
            wartoscZaliczki=Double.valueOf(zaliczka.getValue());
        return "Do wypłaty: "+(sumaKosztow-wartoscZaliczki);
    }

    private String getPobranaZaliczka() {
        if (zaliczka.getValue().equals(""))
            return "Pobrana zaliczka: 0 zł";
        return "Pobrana zaliczka: "+zaliczka.getValue()+" zł";
    }

    private String getSuma() {
        sumaKosztow = kosztPrzejazdu+kosztDiet;
        if (!cenaBiletu.getValue().equals(""))
            sumaKosztow -= Double.valueOf(cenaBiletu.getValue());
        return "Suma kosztów: "+sumaKosztow+" zł";
    }

    private String getKosztyPrzejzadow() {
        int nr = pojazdy.indexOf(rodzajPojazdu.getValue());
        kosztPrzejazdu=0.0;
        switch(nr)
        {
            case 0: kosztPrzejazdu = Double.valueOf(odleglosc.getValue())*0.52; break;
            case 1: kosztPrzejazdu = Double.valueOf(odleglosc.getValue())*0.82; break;
            case 2: kosztPrzejazdu = Double.valueOf(odleglosc.getValue())*0.45; break;
            case 3: kosztPrzejazdu = Double.valueOf(odleglosc.getValue())*0.30; break;
        }
        return "Koszty przejazdów rozliczanych kilometrówką: "+kosztPrzejazdu+" zł";
    }

    private String getOpisPrzejazdu() {
        if (rodzajPojazdu.getValue()==null)
            return "";
        return rodzajPojazdu.getValue().toString() +" "+ odleglosc.getValue() +" km";
    }

    private String getWysokoscDiet()
    {
        if (!wysokoscDiety.getValue().equals(""))
            kosztDiet = Double.valueOf(wysokoscDiety.getValue());
        else
            kosztDiet=0.0;
        if (hours>=10)
            kosztDiet*=(days+1);
        else
            kosztDiet*=days;
        if (sniadania.getValue().matches("\\d+(\\.\\d+)?"))
            kosztDiet -= Integer.valueOf(sniadania.getValue())*7.5;  //wartosc sniadania przyjeta jako 7,50 zł
        if (obiady.getValue().matches("\\d+(\\.\\d+)?"))
            kosztDiet -= Integer.valueOf(obiady.getValue())*9.0;
        if (kolacje.getValue().matches("\\d+(\\.\\d+)?"))
            kosztDiet -= Integer.valueOf(kolacje.getValue())*7.0;
        return kosztDiet+" zł";
    }

    private void daysHoursMins()
    {
        days = dataPowrotu.getValue().getDayOfYear()-dataWyjazdu.getValue().getDayOfYear();
        hours = dataPowrotu.getValue().getHour()-dataWyjazdu.getValue().getHour();
        mins = dataPowrotu.getValue().getMinute()-dataWyjazdu.getValue().getMinute();
    }

    private String getDateDifference()
    {
        StringBuilder sb = new StringBuilder();
        System.out.println("days hours minutes: "+days+" "+hours+" "+mins);
        if (mins<0)
        {
            hours--;
            mins+=60;
        }
        if (hours<0) {
            days--;
            hours +=24;
        }
        sb.append(days).append("d ").append(hours).append("g ").append(mins).append(" min");
        return sb.toString();
    }

    String getNiceDate(LocalDateTime date)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(date.getYear()).append("-").append(date.getMonthValue()).append("-").append(date.getDayOfMonth()).append(" ").append(date.getHour()).append(":").append(date.getMinute());
        return sb.toString();
    }

}
