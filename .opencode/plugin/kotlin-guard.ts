import type { Plugin } from "@opencode-ai/plugin"

/**
 * kotlin-guard: nudge automático de buenas prácticas Kotlin/Compose.
 *
 * Tras cada `write`/`edit` sobre un fichero `*.kt`, añade al resultado una
 * instrucción para que el agente primario invoque al subagente
 * `@kotlin-buenas-practicas` con alcance del fichero modificado.
 *
 * Nota: opencode no permite a un plugin invocar subagentes directamente;
 * esto es una sugerencia inyectada en contexto, no una ejecución garantizada.
 * La invocación manual (`@kotlin-buenas-practicas ...`) siempre funciona.
 */
export default (async () => {
  return {
    "tool.execute.after": async (input, output) => {
      try {
        const tool = (input as { tool?: string }).tool ?? ""
        if (tool !== "write" && tool !== "edit") return

        const args = (output as { args?: Record<string, unknown> }).args ?? {}
        const rawPath =
          (args["filePath"] as string | undefined) ??
          (args["path"] as string | undefined) ??
          ""
        if (!rawPath.endsWith(".kt")) return

        // Adjunta el recordatorio sin alterar el resultado original.
        const reminder =
          `[kotlin-guard] Fichero Kotlin modificado: ${rawPath}. ` +
          `Invoca al subagente @kotlin-buenas-practicas para auditarlo ` +
          `(alcance: este fichero) contra las buenas prácticas vigentes vía Context7.`

        const out = output as Record<string, unknown>
        if (typeof out["output"] === "string" && out["output"].length > 0) {
          out["output"] = `${out["output"]}\n\n${reminder}`
        } else {
          out["output"] = reminder
        }
      } catch {
        // Nunca romper la ejecución por el guard: fallar en silencio.
      }
    },
  }
}) satisfies Plugin
