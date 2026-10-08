import type { Plugin } from "@opencode-ai/plugin"

/**
 * verifier-guard: recordatorio automático del verificador de specs.
 *
 * Tras cada `write`/`edit` sobre un fichero `specs/*-spec.md`, añade al
 * resultado una instrucción para que el agente primario invoque al subagente
 * `@verifier-auto <path>` cuando el spec esté en `Estado: Aprobado`.
 *
 * Nota: opencode no permite a un plugin invocar subagentes directamente;
 * esto es una sugerencia inyectada en contexto, no una ejecución garantizada.
 * La invocación manual (`@verifier-auto specs/NNN-nombre-spec.md`) siempre funciona.
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
        const normalized = rawPath.replace(/\\/g, "/")
        if (!normalized.includes("specs/") || !normalized.endsWith("-spec.md")) return

        // Adjunta el recordatorio sin alterar el resultado original.
        const reminder =
          `[verifier-guard] Spec editado: ${rawPath}. ` +
          `Si tiene 'Estado: Aprobado', invoca al subagente @verifier-auto ${rawPath} ` +
          `para verificarlo criterio por criterio con la skill /verifier.`

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
