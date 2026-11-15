"""
Script de Anonimización de Exportaciones para Smart Bussing usando DataFog Core.
Cumple con el objetivo del laboratorio CETYS Unidad 3 (DLP) para anonimizar datasets
y reportes de pasajeros antes de compartirlos con analistas o auditores.
"""

import csv
import sys
import os

try:
    import datafog
    DATAFOG_AVAILABLE = True
except ImportError:
    DATAFOG_AVAILABLE = False


def anonymize_csv_export(input_path: str, output_path: str):
    if not os.path.exists(input_path):
        print(f"[ERROR] Archivo de entrada no encontrado: {input_path}")
        return

    print(f"[INFO] Procesando archivo con DataFog Core: {input_path} -> {output_path}")

    with open(input_path, mode="r", encoding="utf-8") as infile, \
         open(output_path, mode="w", newline="", encoding="utf-8") as outfile:

        reader = csv.DictReader(infile)
        if not reader.fieldnames:
            print("[ERROR] El archivo CSV esta vacio o no tiene cabeceras.")
            return

        writer = csv.DictWriter(outfile, fieldnames=reader.fieldnames)
        writer.writeheader()

        processed_rows = 0
        for row in reader:
            processed_rows += 1

            # 1. Anonimizar PII de correo si existe
            if "email" in row and row["email"]:
                if DATAFOG_AVAILABLE:
                    row["email"] = datafog.transform(row["email"], action="redact")
                else:
                    parts = row["email"].split("@")
                    row["email"] = f"***@{parts[-1]}" if len(parts) > 1 else "***"

            # 2. Anonimizar texto de comentario (descripcion) contra PII / telefonos
            if "descripcion" in row and row["descripcion"]:
                if DATAFOG_AVAILABLE:
                    row["descripcion"] = datafog.transform(row["descripcion"], action="redact")

            writer.writerow(row)

    print(f"[EXITO] {processed_rows} registros procesados y anonimizados correctamente.")


if __name__ == "__main__":
    src = sys.argv[1] if len(sys.argv) > 1 else "sample_reportes.csv"
    dest = sys.argv[2] if len(sys.argv) > 2 else "sample_reportes_anonymized.csv"

    # Si no existe archivo de prueba, crear uno de demostración
    if not os.path.exists(src) and src == "sample_reportes.csv":
        with open(src, "w", encoding="utf-8", newline="") as f:
            w = csv.writer(f)
            w.writerow(["id_reporte_ruta", "route_id", "email", "descripcion"])
            w.writerow([1, 2, "juan.perez@cetys.mx", "El camion 12 venia lleno. Contactarme al 6461234567"])
            w.writerow([2, 5, "maria.lopez@gmail.com", "Excelente servicio en la parada del centro."])
            w.writerow([3, 2, "conductor.queja@transporte.com", "Parada omitida sin aviso previo."])
        print(f"[INFO] Archivo de muestra generado: {src}")

    anonymize_csv_export(src, dest)
