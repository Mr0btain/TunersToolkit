@file:OptIn(ExperimentalWasmJsInterop::class, ExperimentalEncodingApi::class)

package org.mr0btain.tunerstoolkit.ui.project

import kotlinx.coroutines.await
import org.mr0btain.applogger.AppLogger
import org.mr0btain.tunerstoolkit.core.project.MemoryProjectStore
import org.mr0btain.tunerstoolkit.core.project.ProjectStore
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.Promise

private const val TAG = "OpfsProjectStore"

/** OPFS when the browser has it all (Safari before 26 can't write to it from a page), memory otherwise */
actual fun platformProjectStore(): ProjectStore =
    if (opfsAvailable()) {
        requestPersistence()
        OpfsProjectStore()
    } else {
        AppLogger.w("$TAG:Init", "This browser has no Origin Private File System, projects only last until the page closes")
        MemoryProjectStore()
    }

/**
 * Projects in the browser's Origin Private File System: real files, private to this site, that stay until the site's
 * data is cleared. Bytes cross between Kotlin and JavaScript as base64, which is one string copy instead of a call
 * per byte
 */
class OpfsProjectStore : ProjectStore {
    override suspend fun read(path: String): ByteArray? =
        opfsRead(path).await<JsString?>()?.toString()?.let { Base64.decode(it) }

    override suspend fun write(path: String, bytes: ByteArray) {
        opfsWrite(path, Base64.encode(bytes)).await<JsAny?>()
    }

    override suspend fun list(directory: String): List<String> =
        opfsList(directory).await<JsString>().toString().split('/').filter { it.isNotEmpty() }.sorted()

    override suspend fun delete(path: String) {
        opfsDelete(path).await<JsAny?>()
    }
}

private fun opfsAvailable(): Boolean = js(
    """typeof navigator !== 'undefined' && !!navigator.storage && typeof navigator.storage.getDirectory === 'function'
        && typeof FileSystemFileHandle !== 'undefined' && typeof FileSystemFileHandle.prototype.createWritable === 'function'"""
)

/** Asks the browser not to clear the site's storage when space runs low; it may say no, and that's fine */
private fun requestPersistence(): Unit = js(
    """{ if (navigator.storage.persist) navigator.storage.persist().catch(() => {}); }"""
)

private fun opfsRead(path: String): Promise<JsString?> = js(
    """(async () => {
        const parts = path.split('/').filter(p => p.length > 0);
        if (parts.length === 0) return null;
        try {
            let dir = await navigator.storage.getDirectory();
            for (let i = 0; i < parts.length - 1; i++) dir = await dir.getDirectoryHandle(parts[i]);
            const file = await (await dir.getFileHandle(parts[parts.length - 1])).getFile();
            const bytes = new Uint8Array(await file.arrayBuffer());
            let text = '';
            for (let i = 0; i < bytes.length; i += 0x8000) text += String.fromCharCode.apply(null, bytes.subarray(i, i + 0x8000));
            return btoa(text);
        } catch (e) {
            if (e && (e.name === 'NotFoundError' || e.name === 'TypeMismatchError')) return null;
            throw e;
        }
    })()"""
)

private fun opfsWrite(path: String, base64: String): Promise<JsAny?> = js(
    """(async () => {
        const parts = path.split('/').filter(p => p.length > 0);
        let dir = await navigator.storage.getDirectory();
        for (let i = 0; i < parts.length - 1; i++) dir = await dir.getDirectoryHandle(parts[i], { create: true });
        const handle = await dir.getFileHandle(parts[parts.length - 1], { create: true });
        const text = atob(base64);
        const bytes = new Uint8Array(text.length);
        for (let i = 0; i < text.length; i++) bytes[i] = text.charCodeAt(i);
        const writable = await handle.createWritable();
        await writable.write(bytes);
        await writable.close();
        return null;
    })()"""
)

private fun opfsList(path: String): Promise<JsString> = js(
    """(async () => {
        const parts = path.split('/').filter(p => p.length > 0);
        let dir = await navigator.storage.getDirectory();
        try {
            for (const part of parts) dir = await dir.getDirectoryHandle(part);
        } catch (e) {
            return '';
        }
        const names = [];
        for await (const entry of dir.keys()) names.push(entry);
        return names.join('/');
    })()"""
)

private fun opfsDelete(path: String): Promise<JsAny?> = js(
    """(async () => {
        const parts = path.split('/').filter(p => p.length > 0);
        let dir = await navigator.storage.getDirectory();
        try {
            for (let i = 0; i < parts.length - 1; i++) dir = await dir.getDirectoryHandle(parts[i]);
            if (parts.length === 0) {
                const names = [];
                for await (const name of dir.keys()) names.push(name);
                for (const name of names) await dir.removeEntry(name, { recursive: true });
            } else {
                await dir.removeEntry(parts[parts.length - 1], { recursive: true });
            }
        } catch (e) {
            if (!(e && e.name === 'NotFoundError')) throw e;
        }
        return null;
    })()"""
)
