# Sketsa Patterns

## Panoramica

**Sketsa Patterns** aggiunge a Sketsa un editor per creare e applicare pattern SVG come riempimento o tratto. Il progetto adotta un modello di **applicazione privata e immutabile**: il pannello serve come editor della bozza, mentre ogni applicazione effettiva crea una nuova definizione `<pattern>` indipendente.

Il manifest dichiara il modulo `kiyut.sketsa.modules.patterns` con specification version **0.3.0** e compilazione Java 11.

## Tipi di pattern

Il pannello include generatori per:

- strisce verticali;
- strisce orizzontali;
- scacchiera;
- punti;
- pattern personalizzati o già esistenti.

Per i pattern gestiti dal plugin sono disponibili due colori, dimensioni e posizione della cella, oltre a una modalità di coordinate assolute o relative.

## Coordinate e unità

Sono supportate due modalità:

- **Absolute (SVG units)** — usa `patternUnits="userSpaceOnUse"` e valori SVG assoluti;
- **Relative (%)** — usa `patternUnits="objectBoundingBox"` e valori relativi/percentuali.

Il pannello converte i valori mostrati quando si cambia modalità e visualizza un'anteprima del pattern in preparazione.

## Modello di applicazione privata

Il comando **Create / Update** prepara la bozza del pattern, ma non modifica retroattivamente i pattern già applicati. Ogni **Apply Fill** o **Apply Stroke** crea invece una nuova definizione con ID univoco, per esempio `checker-1`, `checker-2`, `checker-3`.

Questa architettura evita un effetto collaterale tipico dei pattern condivisi: una modifica successiva nel pannello non può alterare accidentalmente oggetti che avevano ricevuto una versione precedente del pattern.

## Operazioni disponibili

- Preparazione/modifica della bozza di pattern.
- Applicazione del pattern come `fill`.
- Applicazione del pattern come `stroke`.
- Rimozione del pattern dal riempimento.
- Rimozione del pattern dal tratto.
- Rilevamento del pattern già usato dall'oggetto selezionato.
- Lettura delle proprietà sia da stile CSS sia da attributi di presentazione SVG.
- Creazione automatica di `<defs>` quando necessario.

## Undo / Redo

Le operazioni di applicazione usano edit dedicati per mantenere insieme:

1. la definizione `<pattern>` privata creata per quella specifica applicazione;
2. lo stato precedente della proprietà `fill` o `stroke` dell'oggetto.

Undo rimuove quindi esattamente la definizione privata creata dall'operazione e ripristina il paint precedente; Redo ricrea entrambi gli aspetti.

## Struttura SVG

La struttura prodotta segue il modello SVG standard:

```xml
<defs>
    <pattern id="checker-1" ...>
        <!-- primitive SVG che compongono il pattern -->
    </pattern>
</defs>

<rect fill="url(#checker-1)" ... />
```

Le definizioni generate sono normali nodi SVG e rimangono nel documento salvato.

## Sorgenti principali

- `src/kiyut/sketsa/modules/patterns/integration/PatternsPanel.java` — editor, anteprima, generazione dei pattern, applicazione e Undo/Redo.
- `src/kiyut/sketsa/modules/patterns/integration/PatternsIntegrator.java` — integrazione del pannello.
- `src/kiyut/sketsa/modules/patterns/Installer.java` — inizializzazione.

## Ambiente di riferimento

- Sketsa 9.1
- NetBeans 11.3
- Java 11
