# ELDERVALE — FINAL DESIGN & DEVELOPMENT SPECIFICATION

> Documento di riferimento per analisi, progettazione e sviluppo.  
> **Repository:** `Brufen5407/eldervale`  
> **Ambiente noto:** MCreator 2026.1, NeoForge 1.21.1, Java 21  
> **Package noto:** `net.mcreator.eldervale`  
> **Build configurata nel repository:** NeoForge `21.1.232`, Gradle  
> **Stato:** specifica incrementale. Le decisioni esplicite sono vincolanti; i dettagli non ancora decisi devono essere verificati e poi chiariti con questionari raggruppati.

---

## 0. Istruzioni operative obbligatorie per Claude

Agisci come **analista tecnico, architetto software e sviluppatore** del progetto Eldervale. Questo documento è la fonte di riferimento per le decisioni di design qui riportate.

### 0.1 Prima di modificare il codice: audit completo

1. Esamina il repository reale e il branch attivo. Non basarti soltanto su questo documento, su vecchie conversazioni o su supposizioni.
2. Crea una mappa dei sistemi e dei file: classi, statistiche, XP/livelli, mana, abilità, HUD, GUI, mob, loot, equipaggiamento, salvataggi, registrazioni, procedure MCreator, risorse, texture, suoni, configurazioni e dipendenze.
3. Cerca le implementazioni già esistenti, comprese quelle generate da MCreator. Segui i riferimenti tra procedure, eventi, capability/componenti, menu, schermate e registrazioni.
4. Per ogni sistema assegna uno stato:
   - **IMPLEMENTATO**: funziona e soddisfa il requisito.
   - **PARZIALE**: esiste ma manca qualcosa.
   - **ASSENTE**: non esiste.
   - **DA VERIFICARE**: non è possibile confermare il comportamento.
   - **DA CORREGGERE**: esiste ma contraddice una decisione approvata.
5. Individua duplicati, codice morto e risorse inutilizzate, ma **non eliminare nulla durante l'audit**. Prima dimostra che un file o una risorsa non è referenziata e che la sua rimozione è sicura.
6. Verifica compilazione, registrazioni e compatibilità con la versione effettiva del workspace. Non aggiornare NeoForge, Minecraft, Gradle, MCreator o Java senza autorizzazione esplicita.
7. Non assumere che una funzionalità sia assente solo perché non trovi una classe dal nome intuitivo: MCreator può implementarla tramite procedure, eventi o file generati.

### 0.2 Protezione del progetto

- Prima di ogni gruppo di modifiche, controlla branch, stato Git e modifiche locali. Non sovrascrivere modifiche dell'utente.
- Lavora preferibilmente su un branch dedicato o su una copia/commit di sicurezza, secondo il flusso concordato.
- Modifica soltanto i file necessari al compito approvato.
- Non riscrivere sistemi funzionanti per uniformità o preferenza personale.
- Non cambiare bilanciamento, statistiche, ID, nomi registrati, formato dei dati salvati o comportamento delle procedure senza motivazione e approvazione.
- Preserva compatibilità con i mondi e i dati esistenti. Se serve una migrazione, progetta prima una strategia compatibile e documentala.
- Non introdurre librerie o dipendenze nuove se una soluzione compatibile con lo stack esistente è sufficiente.
- Dopo ogni gruppo di modifiche, esegui i controlli disponibili e riporta file modificati, motivazione, risultati e rischi.
- Se un requisito è ambiguo, non inventare una regola di gioco: inseriscilo nel questionario di chiarimento descritto al capitolo 13.
- Non dichiarare completato un sistema soltanto perché il codice compila: verifica il flusso e i casi limite.

### 0.3 Prima consegna richiesta a Claude

Prima di implementare nuove funzionalità, consegna:
1. Mappa dei sistemi e dei file rilevanti.
2. Tabella dello stato IMPLEMENTATO/PARZIALE/ASSENTE/DA VERIFICARE/DA CORREGGERE.
3. Elenco dei conflitti tra codice esistente e questo documento.
4. Elenco dei parametri ancora da definire.
5. Questionario completo e raggruppato per argomento, secondo il capitolo 13.

**Non iniziare modifiche sostanziali finché l'utente non ha approvato l'audit e risposto alle domande bloccanti.** Puoi invece proporre un piano tecnico senza modificare il progetto.

---

## 1. Gerarchia delle informazioni

Quando trovi informazioni diverse, applica questo ordine:

1. Decisioni esplicite dell'utente in questo documento.
2. Decisioni esplicite più recenti dell'utente in conversazione, se chiaramente più aggiornate.
3. Comportamento verificato nel repository, per descrivere ciò che è già implementato (non per cambiare il design approvato).
4. Documentazione tecnica ufficiale della versione in uso.
5. Proposte di Claude, che devono essere etichettate come proposte e approvate prima di diventare regole.

Non presentare mai un'ipotesi come una decisione presa.

### Etichette da usare

- **[DECISO]** requisito approvato.
- **[ESISTENTE]** comportamento individuato nel codice.
- **[DA VERIFICARE]** comportamento non ancora confermato.
- **[DA DECIDERE]** scelta che richiede risposta dell'utente.
- **[PROPOSTA]** idea suggerita da Claude, non ancora approvata.

---

## 2. Visione e direzione del progetto

Eldervale è una mod Minecraft con ambizione MMORPG/RPG: progressione del personaggio, classi, statistiche, abilità, mob con livelli, equipaggiamento, rarità, potenziamento, durabilità, professioni, raccolta, crafting ed economia.

La priorità è costruire sistemi coerenti e integrati, non aggiungere feature isolate. Ogni sistema deve riutilizzare i dati e i meccanismi esistenti quando possibile.

### Direzione visiva

La direzione attualmente scelta per le schermate RPG è **spettacolare MMORPG / Fantasy Glass RPG**:
- pannelli semi-trasparenti con tonalità e gradienti multicolore;
- bordi, icone e pulsanti con glow visibile;
- cornici decorative metalliche, riflessi, ombre e profondità;
- palette vivace con blu elettrico, viola, ciano, magenta e oro;
- icone illustrate e leggibili;
- coerenza tra HUD, menu, inventari e finestre RPG.

La leggibilità e la gerarchia delle informazioni hanno precedenza sugli effetti. Evita un risultato generico o visivamente disordinato. Non cambiare la logica di gioco durante un intervento esclusivamente grafico.

---

## 3. Piattaforma e vincoli tecnici noti

- **[ESISTENTE/DA RICONTROLLARE]** MCreator 2026.1.
- **[ESISTENTE/DA RICONTROLLARE]** Minecraft 1.21.1, NeoForge.
- **[ESISTENTE]** build Gradle con plugin NeoForged ModDev; il file `build.gradle` letto durante la preparazione indicava NeoForge `21.1.232` e Java toolchain 21.
- **[ESISTENTE]** package Java noto: `net.mcreator.eldervale`.
- Verificare sempre questi valori nel repository prima di usare API o documentazione.

Non migrare il progetto a un'altra versione per comodità. Non rimuovere o rinominare file generati da MCreator senza capire come MCreator li rigeneri.

---

## 4. Personaggio, classi e progressione

### 4.1 Regole generali

- **[DECISO in precedenti definizioni]** livello massimo del personaggio: 100.
- Le statistiche del personaggio **non aumentano automaticamente salendo di livello**: non introdurre crescita automatica delle statistiche senza una decisione esplicita.
- Le statistiche finali devono distinguere il valore base del personaggio dal contributo dell'equipaggiamento. Regola già espressa: **valore finale = valore base + valore aggiunto dall'equipaggiamento**.
- Prima di aggiungere attributi, controllare quelli già presenti in Eldervale.
- Non reintrodurre il critico vanilla se il sistema è stato rimosso o sostituito: verificare l'implementazione attuale.

### 4.2 Statistiche di classe note

I seguenti valori sono stati forniti dall'utente come riferimento del progetto. Verificarli nel codice e preservare le differenze. Non interpretarli come valori di bilanciamento universalmente definitivi se il repository mostra modifiche successive.

| Classe | HP | DIF | AS | ATT | MANA | Rigenerazione mana | SPEED |
|---|---:|---:|---:|---:|---:|---:|---:|
| Paladin | 50 | 10 | 4.0 | 1.5 | 80 | 4/s | 0.09 |
| Berserker | 35 | 4 | 3.2 | 2.5 | 50 | Da verificare | Da verificare |
| Ranger | 25 | Da verificare | Da verificare | 1.2 | 120 | Da verificare | Da verificare |
| Mage | 20 | Da verificare | Da verificare | 0.5 | Da verificare | Da verificare | Da verificare |
| Assassin | 25 | Da verificare | Da verificare | 1.5 | Da verificare | Da verificare | Da verificare |
| Monk | 35 | Da verificare | Da verificare | 1.8 | Da verificare | Da verificare | Da verificare |
| Werewolf | 40 | Da verificare | Da verificare | 1.7 | Da verificare | Da verificare | Da verificare |
| Alchemist | 20 | Da verificare | Da verificare | 0.7 | Da verificare | Da verificare | Da verificare |
| Spellblade | 30 | Da verificare | Da verificare | 1.7 | Da verificare | Da verificare | Da verificare |

**Importante:** AS, ATT, DIF, SPEED e ogni altra statistica devono mantenere la semantica già usata dal progetto. Non reinterpretare unità o formule senza prima verificarle.

### 4.3 Sistemi di classe già definiti

Le abilità di classe sono state dichiarate completate in una fase precedente. Prima di intervenire, inventaria tutte le abilità esistenti e le relative procedure. Non sostituirle o crearne copie.

Esempi di concetti già definiti da preservare e verificare:
- Paladin: Sacred Aegis (4 scudi per 5 secondi), Divine Blessing, passiva Consecrated Ground/campo circolare 6×6.
- Berserker: Furia incontrollata, Onda d'urto, Berserk Mode (moltiplicatore 1,5× al danno inflitto e ricevuto più velocità), Slam/charge.
- Monk: Martial Artist/combo; preghiera con knockback e cura per 5 secondi.
- Assassin: concetto di backstab/teletrasporto e smoke bomb (raggio 4, invisibilità 5 secondi e perdita dell'aggro); verificare eventuali problemi Java già noti.
- Le altre classi hanno abilità già realizzate: documentare quelle effettive dal codice, non inventarne di nuove.

Il sistema HUD deve mostrare cooldown e costo in mana delle abilità per le classi, con attenzione alla coerenza tra le implementazioni già presenti (in precedenza Paladin e Berserker erano già coperti).

---

## 5. Mondo, mob e progressione PvE

### 5.1 Mondo

- **[DECISO in precedenti definizioni]** mondo principale custom pianificato con dimensione indicativa di circa 10.024 × 10.024 blocchi.
- Il villaggio centrale è il punto iniziale, non va chiamato capitale se non è stato deciso.
- Le dimensioni vanilla devono rimanere accessibili dall'inizio.
- Il mondo dovrebbe avere zone/isole con progressione e varietà, con ispirazione a mondi fantasy di MMORPG/anime; non copiare contenuti protetti.
- Verificare se il world generation è già implementato o è ancora pianificato.

### 5.2 Livelli dei mob

Tabella di progressione fornita in precedenza:

| Distanza/area indicativa | Livello mob |
|---:|---:|
| 0 | 1 |
| 1.000 | 15 |
| 1.500 | 30 |
| 2.000 | 45 |
| 2.500 | 60 |
| 3.000 | 70 |
| 3.500 | 80 |
| 4.000 | 90 |
| 4.500+ | 100 |

Verificare nel codice come viene calcolata la distanza e come vengono gestite le transizioni. Non assumere che la tabella sia già implementata esattamente.

### 5.3 XP e ricompense

Esaminare sistema attuale di livello/XP del giocatore e XP dei mob. Mantenere compatibilità con il livello massimo 100 e con la regola che le statistiche non crescono automaticamente. Le formule XP, curve, cap e ricompense non specificate vanno incluse nel questionario.

---

## 6. Equipaggiamento, statistiche e rarità

### 6.1 Regole di equipaggiamento

- **[DECISO]** tutte le armi sono utilizzabili da tutte le classi.
- Le armi modificano le statistiche del personaggio, non impongono un sistema separato di danno fisso per classe.
- Il danno e le statistiche devono essere calcolati attraverso la struttura già esistente e la regola valore base + bonus equipaggiamento.
- Gli scudi possono aggiungere HP e DIF e possono avere sinergie con il Paladin.
- Prima di creare nuove categorie o slot, verificare quali esistono già.

### 6.2 Bonus degli oggetti

- **[DECISO]** ogni equipaggiamento possiede statistiche/bonus fissi propri dell'oggetto.
- Può inoltre ricevere bonus casuali aggiuntivi.
- Quantità e qualità dei bonus casuali dipendono dalla rarità.
- Gli oggetti più rari hanno maggiori possibilità di ottenere combinazioni potenti.
- **[DECISO]** i bonus casuali non sono modificabili: alla domanda sulla modifica dei bonus è stata scelta l'opzione A, quindi trattarli come permanenti. Non implementare reroll, blocco o sostituzione selettiva senza una nuova approvazione.

### 6.3 Rarità e progressione

La scala di rarità menzionata nel progetto comprende almeno Common, Uncommon, Rare, Legendary, Mythic e categorie superiori quali Divine, Relic, Transcendent e Unique. Verificare i nomi, l'ordine e gli ID effettivamente usati.

- Common e Uncommon: ottenibili dal fabbro NPC.
- Le rarità superiori devono rispettare requisiti di livello e attributi.
- **Mythic e superiori:** possono richiedere requisiti speciali variabili per oggetto: dungeon, raid, boss, grado del giocatore, missioni, reputazione o progressione narrativa.
- I requisiti non devono essere uguali per ogni oggetto: devono essere definiti per il singolo equipaggiamento.
- Per gli oggetti sopra Legendary, il primo utilizzo li vincola permanentemente al giocatore: non sono più scambiabili.
- Gli oggetti fino a Legendary restano scambiabili anche dopo essere stati equipaggiati.
- Gli Unique irripetibili hanno regole specifiche per ogni singolo oggetto: acquisizione, proprietà, scambio, perdita e possibile ricomparsa.
- Tutti gli equipaggiamenti sono scambiabili tranne gli Unique irripetibili, salvo regole specifiche già definite per il singolo Unique.

### 6.4 Ottenimento degli oggetti rari

- **[DECISO]** gli oggetti di rarità elevata possono avere fonti specifiche (boss, dungeon, raid, eventi) oppure drop casuali da contenuti di alto livello.
- Non è stato deciso che ogni oggetto abbia una sola fonte.
- Non aggiungere crafting o scambio come fonte universale per i rarissimi: il metodo di ottenimento deve rispettare le fonti specifiche e il bilanciamento dell'oggetto.
- La probabilità esatta dei drop è ancora da definire.

---

## 7. Potenziamento, riparazione e durabilità

### 7.1 Riferimento di design

L'utente ha richiesto un sistema di durabilità e riparazione ispirato a **Black Desert Online (BDO)**. Usare BDO come riferimento concettuale, non copiare testi, asset o implementazioni. Non presumere che ogni dettaglio di BDO sia stato approvato: valgono soltanto le regole esplicite qui sotto.

### 7.2 Durabilità corrente e massima

Devono esistere due valori distinti:
- **Durabilità corrente:** diminuisce durante l'uso e può essere ripristinata con riparazioni.
- **Durabilità massima:** può diminuire e si recupera separatamente, usando copie dello stesso equipaggiamento o materiali dedicati.

La riparazione della durabilità corrente non deve ripristinare automaticamente la durabilità massima.

### 7.3 Oggetto a durabilità zero

- Quando la durabilità corrente raggiunge 0, l'oggetto diventa inutilizzabile.
- Rimane nell'inventario: non viene distrutto o eliminato.
- Deve avere un effetto grafico rosso luminoso/glow, simile a un cooldown, per far capire immediatamente che è rotto.
- Può essere riparato usando materiali più costosi.
- Una riparazione da 0 deve riportare la durabilità corrente al **50% della durabilità massima attuale**.
- Dopo la riparazione l'oggetto torna utilizzabile.
- Non perdere automaticamente il livello di potenziamento o i bonus dell'oggetto quando si rompe.

### 7.4 Consumo

- **[DECISO]** consumo differenziato per tipo di equipaggiamento e attività.
- Considerare PvE, PvP, esplorazione, utilizzo dell'oggetto, morte e condizioni speciali.
- Le quantità esatte, le eccezioni e la frequenza sono da definire con il questionario.
- Non implementare consumo casuale arbitrario.

### 7.5 Potenziamento

Il sistema di enhancement è previsto nel design generale del gear e deve essere verificato nel repository. Le regole numeriche non sono tutte disponibili in questo documento. Claude deve censire ciò che esiste e chiedere in un unico blocco le decisioni mancanti: livelli massimi, tassi di successo, fallimenti, eventuale downgrade, materiali, protezioni e costi.

---

## 8. Crafting, professioni e raccolta

### 8.1 Crafting

- **[DECISO]** crafting completo in stile MMORPG.
- Common e Uncommon si ottengono dal fabbro NPC.
- Per Rare e superiori è previsto crafting avanzato, ricette, materiali specifici, professioni e contenuti dedicati.
- Professioni con livelli, esperienza e specializzazioni.
- Qualità variabile degli oggetti creati.
- Ricette rare.
- Materiali provenienti da contenuti specifici.
- Mercato tra giocatori.
- Le formule, la probabilità di qualità e le ricette specifiche sono da definire.

### 8.2 Professioni

- **[DECISO]** professioni separate con specializzazioni.
- Il giocatore può sviluppare più professioni, ma può diventare esperto soltanto in un numero limitato.
- Le professioni non specializzate possono avere limiti da definire.
- Collaborazione e mercato tra giocatori devono avere un ruolo utile per accedere ai prodotti delle altre specializzazioni.
- Non inventare il numero massimo di professioni esperte: chiederlo nel questionario.

### 8.3 Raccolta risorse

- **[DECISO]** sistema MMORPG completo.
- Strumenti specifici.
- Livelli professionali/requisiti adeguati alla risorsa.
- Risorse di rarità differente.
- Zone con materiali esclusivi.
- Nodi di raccolta.
- Possibilità di ottenere materiali rari e molto rari.
- Verificare se esistono già blocchi, drop, strumenti o procedure di raccolta e integrarli, non duplicarli.

---

## 9. Economia e commercio

### 9.1 Mercato

- **[DECISO]** scambio diretto tra giocatori.
- **[DECISO]** mercato globale.
- **[DECISO]** aste.
- **[DECISO]** mercati regionali.
- I prezzi sono influenzati da domanda e offerta.
- La realizzazione deve considerare persistenza dei dati, concorrenza, oggetti duplicati, transazioni atomiche e prevenzione di exploit.

### 9.2 Commissioni

- **[DECISO]** commissioni variabili in base a città/regione, rarità e prezzo.
- Possono esserci vantaggi per le gilde.
- Percentuali, limiti, arrotondamenti e vantaggi specifici sono ancora da definire.
- Evitare che commissioni e trasferimenti permettano duplicazione o perdita involontaria di valuta/oggetti.

### 9.3 Valuta

- **[DECISO]** esiste una sola valuta universale.
- Nome scelto: **Aetherion**.
- Tutte le transazioni, gli acquisti, le riparazioni, il crafting e le commissioni usano la stessa valuta.
- Non introdurre valute regionali o speciali senza nuova approvazione.

### 9.4 Fonti di Aetherion

- **[DECISO]** tutte le fonti seguenti sono previste:
  - vendita di materiali ed equipaggiamenti agli NPC;
  - missioni e incarichi;
  - dungeon, boss e raid;
  - commercio tra giocatori;
  - eventi e attività speciali.
- Le quantità devono essere calibrate in base a difficoltà e progressione.
- La valuta creata dagli NPC e dalle ricompense deve essere bilanciata per evitare inflazione e metodi di farming dominanti.
- Il commercio tra giocatori trasferisce valuta, non deve crearla dal nulla.

---

## 10. UI, HUD e presentazione

Prima di modificare le schermate, censire tutte quelle presenti: menu principale, pulsanti, selezione mondi, menu correlati, HUD, inventari, finestre delle abilità e schermate RPG.

Requisiti:
- mantenere coerenza visiva Fantasy Glass RPG;
- informazioni leggibili e gerarchia chiara;
- indicatori evidenti per cooldown, mana, durabilità e stato rotto;
- non modificare logica, eventi, callback, input o navigazione durante un task solo grafico;
- controllare che le schermate siano coerenti tra loro;
- verificare risorse grafiche inutilizzate prima di proporre pulizia.

Per l'oggetto rotto, l'indicatore rosso/glow deve comunicare lo stato senza rendere l'icona illeggibile. Non rimuovere l'oggetto dall'inventario.

---

## 11. Qualità, test e criteri di completamento

Per ogni sistema implementato, documenta:
1. Requisito coperto.
2. File e componenti modificati.
3. Dati salvati e relativa persistenza.
4. Interazioni con altri sistemi.
5. Casi limite gestiti.
6. Test eseguiti e risultato.
7. Problemi residui.

Casi di test minimi da considerare, quando pertinenti:
- uscita e rientro nel mondo: dati persistenti;
- morte e respawn;
- cambio dimensione;
- cambio classe, se consentito;
- equipaggiamento, rimozione e sostituzione;
- inventario pieno;
- oggetto scambiato, venduto o vincolato;
- durabilità a 1, a 0 e riparazione al 50%;
- durabilità massima ridotta e recuperata;
- giocatore disconnesso durante una transazione;
- mercato con due acquisti simultanei;
- risorse o ricompense non ottenibili due volte tramite exploit;
- salvataggi creati da versioni precedenti.

Non dichiarare una feature completata se sono stati verificati soltanto i casi ideali.

---

## 12. Piano di lavoro raccomandato

Procedi per fasi e richiedi approvazione tra le fasi.

### Fase 1 — Audit senza modifiche
- Analisi repository, branch, file, sistemi e procedure.
- Mappa implementato/parziale/assente/da verificare/da correggere.
- Verifica build.
- Elenco conflitti e rischi.

### Fase 2 — Chiarimenti di design
- Confronta audit e requisiti.
- Prepara un questionario unico, raggruppato per argomento.
- Evita domande già risolte.
- Evidenzia le domande realmente bloccanti.
- L'utente risponde in un unico messaggio, per esempio `1C, 2A, 3D`.

### Fase 3 — Specifica tecnica
- Aggiorna questa documentazione con le risposte approvate.
- Definisci architettura e responsabilità dei sistemi.
- Definisci dati, persistenza, formule e dipendenze.
- Identifica i punti d'integrazione con MCreator e codice Java.
- Definisci piano di migrazione se necessario.

### Fase 4 — Implementazione incrementale
- Un sistema o un gruppo coerente alla volta.
- Prima le fondamenta e i dati condivisi, poi UI e contenuti.
- Non fare refactor estranei al task.
- Verifica build e test dopo ogni gruppo.
- Presenta riepilogo e attendi approvazione.

### Fase 5 — Bilanciamento e rifinitura
- Parametri configurabili ove opportuno.
- Test di progressione e interazione tra sistemi.
- Verifica economia, loot e progressione.
- Controllo UI e accessibilità delle informazioni.

---

## 13. Come Claude deve porre le domande

**Regola essenziale: NON fare una domanda alla volta.** L'utente vuole ricevere un questionario completo e raggruppato, così può rispondere in un solo messaggio.

### 13.1 Formato

Prepara un elenco numerato per argomenti. Per ogni domanda:
- una frase breve che spiega la decisione;
- opzioni A/B/C/D chiare e non sovrapposte, quando possibile;
- indica se è possibile selezionare più opzioni;
- indica la conseguenza pratica delle opzioni se non è ovvia;
- non chiedere nuovamente una decisione già presente nel documento.

Esempio:

**1. Potenziamento — probabilità di successo**
- A. Successo garantito.
- B. Probabilità fissa per livello.
- C. Probabilità variabile per livello.
- D. Altro sistema (specificare).

**2. Durabilità — consumo in esplorazione**
- A. Nessun consumo.
- B. Consumo ridotto.
- C. Consumo uguale al combattimento.
- D. Consumo solo in determinate attività.

**3. Loot — influenza della difficoltà**
- A. La difficoltà non modifica il drop.
- B. Aumenta la quantità.
- C. Aumenta la probabilità di rarità elevata.
- D. Può modificare sia quantità sia probabilità.

Formato di risposta consigliato: `1C, 2B, 3D`. L'utente può aggiungere spiegazioni o risposte personalizzate.

### 13.2 Raggruppamento consigliato

Organizza le domande mancanti in blocchi tematici, per esempio:
1. Potenziamento e enhancement.
2. Statistiche, slot e categorie equipaggiamento.
3. Durabilità e riparazione.
4. Loot, drop rate e ricompense.
5. Crafting e qualità.
6. Professioni e specializzazioni.
7. Raccolta e nodi.
8. Mercato, aste, tasse e limiti.
9. Aetherion, prezzi e bilanciamento economico.
10. Progressione, XP e difficoltà.
11. UI e informazioni da mostrare.
12. Regole server/multiplayer, solo se pertinenti all'architettura effettiva.

Non è obbligatorio creare domande in ogni categoria: includi soltanto i punti che restano davvero irrisolti dopo l'audit.

### 13.3 Domande obbligatorie solo se il repository non risponde

Verifica almeno se sono già definite:
- livelli massimi di potenziamento e tassi di successo;
- comportamento in caso di fallimento del potenziamento;
- materiali e costi per potenziare;
- durabilità massima iniziale e quantità di consumo;
- costo e materiali di riparazione ordinaria;
- metodo e quantità per recuperare durabilità massima;
- drop rate e regole di loot;
- numero di bonus casuali per rarità e pool dei bonus;
- numero massimo di specializzazioni professionali;
- progressione XP delle professioni;
- formule di qualità del crafting;
- commissioni di mercato e vantaggi gilda;
- limiti, scadenze e regole delle aste;
- fonti e quantità di Aetherion;
- formule XP del personaggio e dei mob.

Se una di queste informazioni è già chiaramente definita nel repository o in una decisione approvata, non chiedere di nuovo.

### 13.4 Priorità delle domande

Marca ogni domanda:
- **BLOCCANTE**: serve risposta prima di progettare o implementare correttamente.
- **IMPORTANTE**: serve per completare il sistema, ma si può proseguire con analisi non distruttiva.
- **BILANCIAMENTO**: può essere definita in seguito tramite configurazione e test.

Raccogli tutte le domande in un solo questionario. Non interrompere l'utente con una domanda per messaggio, non creare un'intervista sequenziale e non ripetere una domanda già risposta.

---

## 14. Regole di comunicazione con l'utente

L'utente preferisce istruzioni concrete, brevi e operative, in italiano.

- Spiega cosa hai controllato e cosa hai trovato.
- Quando chiedi decisioni, mostra tutte le domande pertinenti nello stesso messaggio.
- Usa numeri e lettere per rendere facile rispondere.
- Non chiedere di ripetere informazioni già note.
- Non presentare una proposta come se fosse già stata approvata.
- Se l'utente sceglie una soluzione, registra la decisione e non riaprire il punto senza un conflitto concreto.
- Se una modifica è esclusivamente estetica, dichiara esplicitamente che logica e funzionalità non saranno toccate.
- Evita spiegazioni prolisse quando bastano passi precisi.

---

## 15. Registro decisioni e modifiche

Mantieni una sezione aggiornata nel documento con:
- decisione;
- data o riferimento della conversazione, se noto;
- stato (approvata/proposta/superata);
- sistemi e file interessati.

Non cancellare silenziosamente le decisioni precedenti: se una decisione viene sostituita, annota quale regola è stata superata e da quale.

### Decisioni confermate in questa specifica

| Area | Decisione |
|---|---|
| Bonus equipaggiamento | Statistiche fisse + bonus casuali aggiuntivi |
| Modifica bonus casuali | Bonus permanenti, niente reroll deciso |
| Requisiti Mythic+ | Requisiti variabili per singolo oggetto |
| Drop rarità elevata | Fonti specifiche e drop casuali da contenuti alti |
| Unique irripetibili | Regole specifiche per oggetto |
| Scambi | Tutto scambiabile tranne Unique irripetibili |
| Vincolo all'uso | Sopra Legendary, vincolo permanente al primo utilizzo |
| Durabilità | Sistema ispirato a BDO |
| Durabilità a zero | Oggetto inutilizzabile, resta in inventario, glow rosso |
| Riparazione da zero | Ripristino al 50% della durabilità massima attuale, con materiali più costosi |
| Consumo durabilità | Differenziato per equipaggiamento e attività, include morte e PvE/PvP da dettagliare |
| Durabilità massima | Recupero separato tramite copie dello stesso oggetto o materiali dedicati |
| Crafting | MMORPG completo; Common/Uncommon dal fabbro NPC |
| Professioni | Separate, con specializzazioni e numero limitato di professioni esperte |
| Raccolta | Strumenti, livelli, rarità, zone, nodi e materiali rari |
| Mercato | Scambio diretto, mercato globale, aste e mercati regionali |
| Commissioni | Variabili per città/regione, rarità e prezzo, con possibili vantaggi gilda |
| Valuta | Una sola valuta universale: Aetherion |
| Fonti valuta | NPC, missioni, dungeon, boss, raid, commercio ed eventi |

---

## 16. Obiettivo finale

Il risultato deve essere un progetto Eldervale coerente, manutenibile e progressivamente ampliabile.

Il tuo compito non è riscrivere tutto da zero né aggiungere sistemi per iniziativa personale. Il tuo compito è:
1. capire esattamente cosa esiste;
2. preservare ciò che funziona;
3. individuare ciò che manca;
4. raccogliere in un unico questionario le decisioni mancanti;
5. progettare l'integrazione;
6. implementare per fasi verificabili;
7. documentare ogni cambiamento.

**Prima audit, poi questionario completo, poi approvazione, poi implementazione.**
