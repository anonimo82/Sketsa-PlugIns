# Sketsa Switch

## Panoramica

**Sketsa Switch** aggiunge strumenti per creare e gestire elementi SVG `<switch>`, utili per contenuti alternativi selezionati in base a condizioni come `systemLanguage`.

Il modulo è identificato come `kiyut.sketsa.modules.switcher`; il manifest riporta specification version **0.2.6** e il progetto è configurato per Java 11.

## Funzioni principali

Il pannello permette di:

- racchiudere un oggetto SVG selezionato in un nuovo `<switch>`;
- aggiungere un oggetto come alternativa a uno `<switch>` esistente;
- impostare o modificare l'attributo `systemLanguage` dell'alternativa selezionata;
- rimuovere un'alternativa;
- estrarre un'alternativa dal `<switch>` e reinserirla nel documento;
- simulare una lingua per verificare quale alternativa dovrebbe risultare attiva;
- sincronizzare il pannello con la selezione e con le modifiche DOM del documento.

## Gestione di `systemLanguage`

La versione 0.2.6 introduce una protezione specifica per l'editing del campo lingua. Quando l'utente inizia a digitare, il campo viene marcato come **dirty** e la sincronizzazione automatica dal DOM viene temporaneamente sospesa.

In questo modo un timer di aggiornamento non può sovrascrivere il testo non ancora confermato. Premendo **Update Language**, il valore viene scritto nel DOM e lo stato dirty viene azzerato. Da quel momento Undo/Redo può nuovamente aggiornare il campo in base allo stato effettivo del documento.

Il cambio di selezione annulla invece l'eventuale valore non confermato e carica il `systemLanguage` dell'alternativa appena selezionata.

## Struttura SVG

Il plugin opera su strutture standard come:

```xml
<switch>
    <g systemLanguage="it">
        <!-- contenuto italiano -->
    </g>
    <g systemLanguage="en">
        <!-- contenuto inglese -->
    </g>
    <g>
        <!-- fallback -->
    </g>
</switch>
```

Quando un'alternativa viene estratta, il plugin rimuove `systemLanguage` dal nodo estratto e lo riposiziona fuori dallo `<switch>` mantenendone il contenuto.

## Undo / Redo

Le modifiche strutturali vengono raggruppate tramite il `DOMUndoManager` di Sketsa. Creazione del contenitore, aggiunta/rimozione delle alternative, aggiornamento della lingua ed estrazione possono quindi partecipare alla normale cronologia Undo/Redo del documento.

## Integrazione con Sketsa

Il pannello usa il `VectorCanvas` attivo e ascolta la selezione corrente. Una sincronizzazione periodica mantiene coerenti interfaccia e DOM senza interferire con un valore `systemLanguage` ancora in fase di digitazione.

## Sorgenti principali

- `src/kiyut/sketsa/modules/switcher/integration/SwitchPanel.java` — UI, gestione `<switch>`, simulazione lingua e Undo/Redo.
- `src/kiyut/sketsa/modules/switcher/integration/SwitchIntegrator.java` — integrazione nell'interfaccia Sketsa.
- `src/kiyut/sketsa/modules/switcher/Installer.java` — bootstrap del plugin.

## Ambiente di riferimento

- Sketsa 9.1
- NetBeans 11.3
- Java 11
