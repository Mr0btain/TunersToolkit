# Checksums

ECUs check their own flash at start up. Change a map without correcting the checksums that cover it and the ECU may
refuse to run the calibration, or not start at all. Tuners Toolkit verifies and fixes checksums through **modules**:
one per ECU family, built in or added by you.

The code is in `core/.../checksum/`:

- `ChecksumAlgorithm`: the generic algorithms. Every module is built from these.
- `ChecksumCheck` and `ChecksumStore`: one checksum, meaning which ranges it covers and where its result goes.
- `Checksums`: verifies checks and fixes them. A fix is one undo step, and it goes round again when one check's
  result sits inside another's ranges.
- `ChecksumModule` and `DefinedModule`: a family's checks, plus how its binaries are recognised.
- `TtSum`: the `.ttsum` JSON format for modules.
- `ChecksumModules`: the modules built into the app.

The app's **Checksums** screen shows the module in use and every check. You can fix them all there, load a `.ttsum`,
or add checks of your own. Your own checks and any module you picked are saved with the project. Exporting an image
warns you when its checksums are wrong.

## Algorithms

| Algorithm | Settings | Notes |
|---|---|---|
| Sum | `word` 1, 2, 4 or 8 bytes; `width` 8, 16, 32 or 64 bits; `byteOrder`; `init` | The total wraps round at its width. A final partial word is padded with zeros at its end. |
| XOR | `word`, `byteOrder`, `init` | |
| CRC | `width` up to 32, `poly`, `init`, `refIn`, `refOut`, `xorOut` | The usual parameter model. Presets come from the CRC catalogue: CRC-8/SMBUS, CRC-8/SAE-J1850, CRC-16/ARC, /MODBUS, /IBM-3740 (CCITT-FALSE), /XMODEM, /KERMIT, CRC-32/ISO-HDLC (the "standard" CRC-32), /BZIP2, /MPEG-2 and /ISCSI. Each one is tested against its catalogue check value. |
| Adler-32 | | The same as zlib's. |

## Where a result goes

| `form` | Meaning |
|---|---|
| `VALUE` | The checksum is stored at `address`, `size` bytes in `byteOrder`. |
| `COMPLEMENT` | Its inverse (bits flipped) is stored there. |
| `VALUE_AND_COMPLEMENT` | The checksum, then its complement straight after it. |
| `FORCE` | Nothing is stored. A correction word at `address`, inside the ranges, is set so the result comes out as `target`. This is how Bosch MED17/EDC17 blocks work. Sums and XORs need the correction to be one of their words; CRCs are solved bit by bit over GF(2), so any 32-bit word in the range works. |

A check's ranges must not include its own stored value (except with `FORCE`), and must be in the image. A check reads
at most 64 MB.

## Adding a family

### As a .ttsum file (no code)

A family with fixed checks, at the same ranges in every binary of that family, is just a file:

```json
{
  "format": "ttsum", "formatVersion": 1,
  "id": "bosch-example", "name": "Example family", "family": "Bosch ME7",
  "description": "What it covers",
  "status": "UNVERIFIED",
  "source": "Where the algorithm comes from: a URL, a document, your own testing",
  "detect": [
    { "text": "0261", "address": "0x10000" },
    { "bytes": "CAFEBABE", "address": "0x1FFF0" },
    { "size": "0x80000" }
  ],
  "checks": [
    {
      "name": "Calibration CRC",
      "algorithm": { "crc": "CRC-32/ISO-HDLC" },
      "ranges": [ "0x10000-0x1FBFF" ],
      "store": { "address": "0x1FC00", "size": 4, "byteOrder": "LITTLE_ENDIAN", "form": "VALUE" }
    },
    {
      "name": "Data block",
      "algorithm": { "sum": { "word": 4, "width": 32, "byteOrder": "LITTLE_ENDIAN", "init": "0xFADECAFE" } },
      "ranges": [ "0x20000-0x2FFFF" ],
      "store": { "address": "0x2FFFC", "size": 4, "byteOrder": "LITTLE_ENDIAN", "form": "FORCE", "target": "0xCAFEAFFE" }
    }
  ]
}
```

- **Detectors:** every detector must match before a module is offered for an image. A detector is text at an
  address, bytes at an address, or a total image size.
- **Ranges:** each is `first-last`, and both ends are included.
- **Numbers:** decimal, or `0x` hex. Addresses go up to 0xFFFFFFFF.
- **CRCs:** use a preset by name (`{"crc": "CRC-16/XMODEM"}`), or give the parameters
  (`{"crc": "MY-CRC", "width": 16, "poly": "0x1021", "init": "0xFFFF", "refIn": false, "refOut": false, "xorOut": "0x0"}`).
- **Status:** `VERIFIED` only after the module has been checked against real binaries. Fixing with an `UNVERIFIED`
  module asks first.

Load it from the Checksums screen to try it. Once it's right, add it to `ChecksumModules.definedModules` (the text in
a `const val`, like `DEMO`) to build it in, with a test.

### In Kotlin (when blocks have to be found)

Some families keep a table in the binary that says where their checksum blocks are (ME7's multipoint descriptors,
MED17's block headers). For those, implement `ChecksumModule`:

- `detect(image)` says whether the image is this family's, and how sure it is.
- `checks(image)` reads the table and returns `ChecksumCheck`s built from the generic algorithms.

Add the module to `ChecksumModules.codeModules`. Verifying and fixing then work the same as for any other module. If a
family needs an algorithm the generic ones can't express, add it to `ChecksumAlgorithm`, with tests against known
values.

### Verifying a module

1. **Stock binaries first:** with real stock binaries of the family in `testdata/local/` (never committed), every
   check should read **Right**. Write a test that skips when the files aren't there, as `LargeA2lTest` does.
2. **Changed binaries:** change a map, fix the checksums, and compare with what a known good tool produces for the
   same change.
3. **Only then** set `"status": "VERIFIED"`.

## Families researched so far

None of these is built in yet. Each needs its algorithm confirmed against real binaries before it can be verified.
This is a starting point for whoever has the binaries, not an implementation.

| Family | What's publicly described | Source |
|---|---|---|
| Bosch MED17 / EDC17 | Three algorithms, each starting from 0xFADECAFE: ADD32 (32-bit sum of dwords) and ADD16 (sum of 16-bit words) must come to 0xCAFEAFFE, and CRC32 (reflected 0xEDB88320) must come to 0x35015001. All are corrected through the last 4 bytes of each region, which is `FORCE` here. The README names the block types (0x10 startup, 0x30 customer, 0x40 application, 0x60 dataset, 0xC0 absolute constants) but not how the blocks are found, so this needs a Kotlin module. CVN and monitoring checksums are extra. | [medc17-checksum-tool](https://github.com/ConnorHowell/medc17-checksum-tool) (MIT) |
| Bosch ME7.2 (Porsche 986/996) | Fixed offsets: a calibration CRC32 ("standard") over 0x10000-0x1FC03, stored at 0x1FC0E; ROMSYS startup, parameter page and program page sums; two multipoint descriptor tables, at 0x17D26 and 0x1EAA6. Byte order and the descriptor layout aren't given. Detected by the Bosch part number block at 0x1FCBE. | [ME7Sum](https://github.com/nyetwurk/ME7Sum) (BSD) |
| Bosch ME7.1 / ME7.5 (VAG) | Supported by ME7Sum, but its README doesn't describe the algorithms. | [ME7Sum](https://github.com/nyetwurk/ME7Sum) |
| Bosch Motronic (BMW DME, M1.x/M3.x era) | A correction tool exists; its README doesn't describe the algorithm. | [motronic-checksum](https://github.com/matiss/motronic-checksum) (MIT) |
| Bosch EDC15 / EDC16 | Nothing open was found. Commercial tools cover them. | |

When adding one, read the sources' code for the algorithm, write it as a `.ttsum` or a Kotlin module in our own words
(don't copy their code), and keep their licence in mind. MIT and BSD sources ask for their notice when code is
copied.
