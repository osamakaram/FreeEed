# FreeEed Standard ESI Production Format

This specification defines a **court-ready ESI production** that FreeEed can emit so its
output drops directly into any standard review platform. It is written in FreeEed's own
terms and abstracted from a representative court-ordered ESI protocol (a public,
GPO-authenticated *Stipulated Order Re: ESI*, N.D. Cal., GovInfo
`USCOURTS-cand-4_21-cv-08787-11`). It is not specific to any matter.

Tracking issue: [#551](https://github.com/shmsoft/FreeEed/issues/551).

## 1. Goals

- Produce a set that satisfies a typical court ESI production order with no manual rework.
- Be load-platform agnostic via standard load files (Concordance `.DAT` + Opticon `.OPT`).
- Keep the production self-describing: images, text, natives, and metadata that tie together by Bates number.

## 2. Bates numbering

- **Format:** `<PREFIX>-<NNNNNNNNN>` — a producing-party prefix, a single dash, then a
  zero-padded number that is a **constant nine digits** across the whole production
  (e.g. `ABC-000000123`).
- **Unique** across the entire production; **sequential within a document**.
- **Page-level:** every produced image page gets its own Bates number.
- The **prefix** is configurable per production/producing party.
- Bates is the join key across images, text, natives, and the metadata load file.

> FreeEed's existing UPI scheme (`UPI_00000`) is the internal identifier; this spec adds a
> configurable, spec-compliant Bates number for production output.

## 3. Images

- **Single-page**, **black & white**, **Group IV TIFF**.
- **≥ 300 dpi**; page size **8.5 × 11"** unless the document reasonably requires otherwise.
- Preserve the source **orientation** (portrait/landscape).
- Each TIFF is **branded** with its Bates number and **named** with that same page-level
  Bates number (e.g. `ABC-000000123.tif`).

## 4. Text

- **One multipage `.TXT` per document** (not one file per page).
- Named with the document's **beginning Bates** number (e.g. `ABC-000000123.txt`).
- Content is **extracted directly from the native** file; when a document is **redacted** or
  is **hard copy**, generate the text by **OCR** of the redacted/scanned image.

## 5. Native files

Produce the following **natively** (whether standalone, attachments, or embedded):

- **Spreadsheets** (`.xls`/`.xlsx`/`.csv` and similar), **Microsoft Access**,
- **Audio/Video** (e.g. `.wav`, `.mpeg`).

For each native produced:

1. Provide a **single-page TIFF placeholder** branded with a Bates number, stating
   **"Document Produced in Native."**
2. **Name** the native file with the Bates number on its placeholder.
3. Still provide the **text and metadata** (including original file name) for the native.

Special cases:

- **Redacted spreadsheets** may be produced natively (redacted) or TIFFed so all
  non-redacted content — including hidden rows/columns and comments — is shown.
- **Error / corrupt / password-protected** files that cannot be TIFFed → produce **native** +
  placeholder.
- **Container files** (`.zip`, `.pst`) need not be produced if their responsive contents are.

## 6. Load files

Every production is accompanied by:

- A **data load file** — delimited (Concordance `.DAT` style), one row per document, columns =
  the metadata fields in §8.
- An **image load file** — **Opticon `.OPT`** style, mapping Bates numbers to image paths and
  marking document boundaries.

## 7. Family relationships & de-duplication

- **Families:** preserve parent/child (email ↔ attachments) by assigning **sequential Bates**
  within the family and recording accurate **attachment ranges** (`PRODBEGATT` / `PRODENDATT`).
- **De-duplication:** remove **exact duplicates by MD5**, on a **message/family** basis. List
  **all custodians** who held a copy (`CUSTODIAN` + `DUPCUSTODIAN`). Email **threading**
  (suppressing fully-contained messages) is optional; disclose the method on request.

## 8. Metadata fields

Dates are `MMDDYYYY`; times are `HH:MM:SS`. `HASH` is MD5 or SHA-1. Fields are populated to the
extent present in the source and customarily extractable.

### 8.1 Electronic files (Exhibit A)

| Field | Description |
|---|---|
| `PRODBEG` | First Bates number of the document |
| `PRODEND` | Last Bates number of the document |
| `PRODBEGATT` | First Bates number of the first item in a parent/child family |
| `PRODENDATT` | Last Bates number of the last item in a parent/child family |
| `ATTACH_COUNT` | Number of attachments to an email or loose e-file with extracted children |
| `PRODVOL` | Production volume containing the file |
| `PRODPARTY` | Producing party |
| `CUSTODIAN` | Person from whom the file was collected, reviewed, and produced |
| `DUPCUSTODIAN` | Additional custodians from whom the email/document was collected |
| `DOCTYPE` | Hard Copy, E-Mail, Attachment, or E-Docs (loose/standalone) |
| `FROM` | Names + SMTP addresses on the From line (email/calendar) |
| `TO` | Names + SMTP addresses on the To line |
| `CC` | Names + SMTP addresses on the CC line |
| `BCC` | Names + SMTP addresses on the BCC line |
| `SUBJECT` | Email subject |
| `DATE_SENT` | Date sent (`MMDDYYYY`) |
| `TIME_SENT` | Time sent (`HH:MM:SS`) |
| `LINK` | Link to the native file on the produced media |
| `FILE_EXTEN` | File extension of the email/attachment/loose e-file |
| `FILE_NAME` | File name of the attachment or loose e-file |
| `FILE_PATH` | Original file path of the email or loose e-file |
| `AUTHOR` | Author of the loose e-file or e-file attachment |
| `DATE_CREATED` | Created date (`MMDDYYYY`) |
| `DATE_MODIFIED` | Last-modified date (`MMDDYYYY`) |
| `REDACTION` | Yes/No — contains redactions |
| `CONFIDENTIALITY` | Confidentiality designation, if any |
| `HASH` | MD5 or SHA-1 hash generated during processing |
| `PASSWORD` | Yes/No — password protected |

### 8.2 Hard-copy documents (Exhibit B)

| Field | Description |
|---|---|
| `PRODBEG` | First Bates number |
| `PRODEND` | Last Bates number |
| `PRODVOL` | Production volume |
| `PRODPARTY` | Producing party |
| `CUSTODIAN` | Person from whom collected, reviewed, and produced |
| `DOCTYPE` | Hard Copy, E-Mail, Attachment, or E-Docs |
| `REDACTION` | Yes/No — contains redactions |
| `CONFIDENTIALITY` | Confidentiality designation, if any |

## 9. Redactions

- Use **labeled** redaction boxes stating the basis (e.g. *"Redacted for Privilege"*);
  **no black-box** redactions.
- Redacted documents: produce **OCR** text of the redacted image (not the native text).
- **Wholly privileged family members:** produce a Bates-branded TIFF **placeholder**
  ("Document Withheld as Privileged") and populate the metadata fields.

## 10. Volumes & delivery

- Organize output into **volumes**, each with a **sequential volume number** identifying the
  producing party (`PRODVOL`).
- Deliverable on **USB media** or via **secure transfer**; accompany with a cover note listing
  the volume(s) and the Bates range(s) per volume (including known gaps).

## 11. Output layout (proposed)

```
VOL001/
  IMAGES/        ABC-000000001.tif ...      (single-page Group IV TIFF, Bates-named)
  TEXT/          ABC-000000001.txt ...      (one multipage .txt per document)
  NATIVES/       ABC-000000050.xlsx ...     (Bates-named natives)
  DATA/
    VOL001.DAT                              (delimited metadata load file, fields per §8)
    VOL001.OPT                              (Opticon image load file)
```

## 12. FreeEed integration notes

- Reuse and extend: `metadata/ColumnMetadata.java`, `MetadataWriter.java`, `LoadDiscovery/`.
- Add a configurable **Bates prefix** and 9-digit zero-pad numbering alongside the UPI scheme.
- Add a selectable **"Standard ESI production" profile** so a user emits a compliant set in one
  step (images + text + natives + `.DAT`/`.OPT` + Exhibit A/B metadata).
- Must remain **forensically sound** — no outbound calls during production.
