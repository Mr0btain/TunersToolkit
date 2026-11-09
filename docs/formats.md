# Formats

Every format Tuners Toolkit reads and writes, where its code lives in `core`, and the specification it follows. All
readers share a few rules:

- **The format is recognised from the content**, never the file name (`BinaryFormat.detect`, and the definition and
  value readers each check their own content).
- **Readers never throw.** Anything they can't read is left out and reported as a `Diagnostic` with its line, column
  and object name. Only a file with nothing usable in it fails (`ImportError`). `FuzzTest` feeds random and mutated
  files to every reader to keep it that way.
- **Imported files are untrusted.** XML nesting is capped (`XmlParser.MAX_DEPTH`, 256), DTDs are skipped rather than
  read (so there are no external entities), JSON nesting is capped (`TtDef.MAX_NESTING`), and a checksum reads at
  most 64 MB.

## Images

The code is in `core/.../formats/binary/`. Every image becomes a `MemoryImage`: sorted segments of bytes with gaps
between them, at addresses up to 0xFFFFFFFF.

| Format | Code | Read | Write | Spec |
|---|---|---|---|---|
| Intel HEX | `IntelHex` | Record types 00 (data), 01 (end), 02 and 04 (segment and linear address), 03 and 05 (start). Each record's checksum is checked | 16 bytes a record by default (1 to 255), with 04 extended linear address records and a 05 start record when there's a start address | *Hexadecimal Object File Format Specification*, Intel, rev. A, 1988 |
| Motorola S-record | `SRecord` | S0 header, S1/S2/S3 data, S5/S6 count, S7/S8/S9 start. Each record's checksum is checked | S0, then the smallest address width that fits the image, a count record when it fits, and the matching end record | `srec(5)`, from Motorola's *M68000 Family Programmer's Reference Manual*, appendix C |
| Raw binary | `RawBinary` | The whole file at a base address you give (often 0, or the A2L's `MEMORY_SEGMENT`) | Gaps filled with 0xFF | None: a plain dump (`.bin`, `.ori`) |

When pieces overlap, the last one read wins, and `MemoryImage.Builder` lists every overlap in the import report.

## Definitions

A definition says where each map is in the image and how to turn its bytes into numbers. Every format is resolved
into the same model, a `CalibrationSet` of `CalibrationObject`s (`core/.../calibration/`), so everything after
reading works the same whichever format it came from.

### ASAM A2L

`core/.../a2l/`. It follows **ASAM MCD-2 MC (ASAP2) 1.5.1 to 1.7.1**
([asam.net/standards/detail/mcd-2-mc](https://www.asam.net/standards/detail/mcd-2-mc/)).

- `A2lText` picks the encoding (UTF-8 with or without a BOM, UTF-16, or Latin-1). `A2lLexer` streams tokens with
  their line and column, and `A2lParser` builds a tree from `A2lSpec`, the table of every keyword's parameters. To
  support a new keyword, add it there, not in the parser. Unknown keywords are kept and warned about.
- A2ML is parsed (`A2ml`), and IF_DATA is kept as its raw text, so writing the file back loses nothing.
- `A2lModelBuilder` makes the typed model, and `A2lResolver` turns a `MODULE` into a `CalibrationSet`. It covers:
  - every characteristic kind (VALUE, CURVE, MAP, CUBOID, CUBE_4, CUBE_5, VAL_BLK, ASCII) and AXIS_PTS
  - every RECORD_LAYOUT field, with inline, shared (COM_AXIS, RES_AXIS, CURVE_AXIS) and fixed axes
  - every COMPU_METHOD, including FORM formulas (`Expression`), the COMPU_TAB/VTAB/VTAB_RANGE tables, and
    `ECU_CALIBRATION_OFFSET`
- `EpkCheck` compares MOD_PAR's EPK with the bytes at `ADDR_EPK`, so you know whether the A2L belongs to the image.
- `A2lWriter` writes a tree, or a subset of objects, back to A2L text.
- `/include` is supported by `A2lImport` through an `A2lFileResolver`. The app doesn't give it one yet, so included
  files are skipped with a warning.

### TunerPro XDF

`core/.../formats/xdf/Xdf`. TunerPro's own XML format ([tunerpro.net](https://www.tunerpro.net/)). There is no
published specification, so the reader follows what TunerPro writes:

- constants, tables with their axes, and flags, at offsets from the start of the binary, plus or minus `BASEOFFSET`
- `mmedtypeflags`: 0x01 signed, 0x02 LSB first, 0x04 column major, 0x10000 floating point
- `MATH` equations in `X`, where `^` is a power

Equations that read other objects, strided data, and the XDF's own checksums and patches aren't supported. They're
reported and left out.

### `.ttdef`

`core/.../formats/ttdef/TtDef`: Tuners Toolkit's own JSON definition. It holds the calibration model as it is, so a
definition made in the app (by hand or from the map finder), or converted from another format, reads back exactly.
Addresses are hex strings, and limits that aren't set are left out. A project keeps the maps you made by hand as
`additions.ttdef`.

## Calibration values

These carry values **by name**, not addresses (`core/.../formats/values/`). `CalibrationData` turns objects into
`DataRecord`s through the open definition. Importing writes every record back as one undo step, and the report lists
names that aren't in the definition, have the wrong size, or are outside the map's limits.

| Format | Code | Read | Write | Spec |
|---|---|---|---|---|
| DCM | `Dcm` | ✅ | ✅ | DAMOS DCM 2.0 (`KONSERVIERUNG_FORMAT 2.0`, ETAS): FESTWERT, FESTWERTEBLOCK, KENNLINIE, FESTKENNLINIE, GRUPPENKENNLINIE, KENNFELD, FESTKENNFELD, GRUPPENKENNFELD, STUETZSTELLENVERTEILUNG and TEXTSTRING |
| CDF / CDFX | `Cdfx` | ✅ | ✅ | ASAM CDF 2.0 ([asam.net/standards/detail/cdf](https://www.asam.net/standards/detail/cdf/)): one SW-INSTANCE per object, with physical values and axes |
| PaCo | `Cdfx` | ✅ | | ETAS's XML, which keeps its values in the same SW-INSTANCE structure |
| CSV | `Csv` | ✅ | ✅ | A map as a spreadsheet grid: x points across the first row, y points down the first column. Written with semicolons; read with tabs, semicolons or commas, and with decimal commas |

The map list can also be exported as CSV (Files screen, definition panel).

## Checksum modules

`.ttsum` is JSON for a checksum module: what recognises the family's images, and its checks. See
[checksums.md](checksums.md).

## Not supported

WinOLS `.kp` and `.ols` files are proprietary and undocumented, so they aren't read. Use A2L, XDF or CSV instead.
VBF, ODX/FRF/SGO flash containers and MDF logs may come later (see [PLAN.md](PLAN.md) Section 7).
