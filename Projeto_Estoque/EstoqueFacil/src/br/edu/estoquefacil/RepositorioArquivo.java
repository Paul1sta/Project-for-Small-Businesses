package br.edu.estoquefacil;

import br.edu.estoquefacil.Modelo.*;
import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.zip.CRC32;
import static java.nio.file.StandardOpenOption.*;

/** Arquivo binário versionado, com CRC32 e gravação por substituição atômica. */
public final class RepositorioArquivo implements Repositorio {
    private static final int MAGIC = 0x45464442;
    private static final int VERSAO = 1;
    private static final int TAMANHO_MAXIMO = 64 * 1024 * 1024;
    private final Path caminho;
    private final FileChannel canal;
    private final FileLock trava;
    private boolean fechado;

    public RepositorioArquivo(Path destino) throws IOException {
        caminho = destino.toAbsolutePath().normalize();
        Files.createDirectories(caminho.getParent());
        canal = FileChannel.open(caminho.resolveSibling(caminho.getFileName() + ".lock"), CREATE, WRITE);
        FileLock obtida;
        try {
            obtida = canal.tryLock();
            if (obtida == null) throw new IOException("A base já está aberta em outra janela. Feche a outra instância.");
        } catch (IOException | OverlappingFileLockException e) {
            canal.close();
            throw new IOException("Não foi possível abrir a base. Ela pode estar em uso por outra instância.", e);
        }
        trava = obtida;
    }
    public Path caminho() { return caminho; }
    private void aberto() throws IOException { if (fechado) throw new IOException("Base fechada."); }
    @Override public Dados carregar() throws IOException {
        aberto();
        return Files.exists(caminho) ? ler(caminho) : Dados.vazio();
    }
    @Override public void salvar(Dados dados) throws IOException {
        aberto();
        byte[] bytes = serializar(dados);
        Path temporario = Files.createTempFile(caminho.getParent(), "estoque-", ".tmp");
        try {
            escrever(temporario, bytes, false);
            // Se o sistema de arquivos não suporta ATOMIC_MOVE, a operação falha sem publicar o novo estado.
            Files.move(temporario, caminho, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporario); }
    }
    private static void escrever(Path arquivo, byte[] bytes, boolean novo) throws IOException {
        try (FileChannel out = novo ? FileChannel.open(arquivo, CREATE_NEW, WRITE) : FileChannel.open(arquivo, WRITE, TRUNCATE_EXISTING)) {
            java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) out.write(buffer);
            out.force(true);
        }
    }
    /** Backup com nome novo, para não sobrescrever uma cópia anterior por engano. */
    public static void backup(Path destino, Dados dados) throws IOException { escrever(destino, serializar(dados), true); }
    static byte[] serializar(Dados dados) throws IOException {
        Modelo.validar(dados);
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(payload)) {
            out.writeInt(dados.produtos().size());
            for (Produto p : dados.produtos().values()) {
                out.writeUTF(p.codigo()); out.writeUTF(p.nome()); out.writeUTF(p.categoria());
                out.writeLong(p.custoCentavos()); out.writeInt(p.minimo());
            }
            out.writeInt(dados.movimentos().size());
            for (Movimento m : dados.movimentos()) {
                out.writeLong(m.id()); out.writeLong(m.instante().toEpochMilli());
                out.writeUTF(m.produto()); out.writeUTF(m.nomeNaData()); out.writeByte(m.tipo().ordinal());
                out.writeInt(m.quantidade()); out.writeUTF(m.motivo());
            }
        }
        byte[] corpo = payload.toByteArray();
        if (corpo.length > TAMANHO_MAXIMO) throw new IOException("Arquivo excedeu o limite de 64 MiB.");
        CRC32 crc = new CRC32(); crc.update(corpo);
        ByteArrayOutputStream arquivo = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(arquivo)) {
            out.writeInt(MAGIC); out.writeInt(VERSAO); out.writeInt(corpo.length); out.write(corpo); out.writeLong(crc.getValue());
        }
        return arquivo.toByteArray();
    }
    public static Dados ler(Path arquivo) throws IOException {
        if (Files.size(arquivo) > TAMANHO_MAXIMO + 20L) throw new IOException("Arquivo muito grande.");
        try (DataInputStream in = new DataInputStream(Files.newInputStream(arquivo))) {
            if (in.readInt() != MAGIC || in.readInt() != VERSAO) throw new IOException("Formato ou versão de arquivo incompatível.");
            int tamanho = in.readInt();
            if (tamanho < 0 || tamanho > TAMANHO_MAXIMO) throw new IOException("Tamanho de arquivo inválido.");
            byte[] corpo = in.readNBytes(tamanho);
            if (corpo.length != tamanho) throw new IOException("Arquivo incompleto.");
            CRC32 crc = new CRC32(); crc.update(corpo);
            if (crc.getValue() != in.readLong() || in.read() != -1) throw new IOException("Arquivo corrompido ou com conteúdo adicional.");
            try (DataInputStream dados = new DataInputStream(new ByteArrayInputStream(corpo))) {
                int produtos = contagem(dados.readInt(), Modelo.LIMITE_PRODUTOS);
                Map<String, Produto> ps = new LinkedHashMap<>();
                for (int i = 0; i < produtos; i++) {
                    Produto p = new Produto(dados.readUTF(), dados.readUTF(), dados.readUTF(), dados.readLong(), dados.readInt());
                    if (ps.put(p.codigo(), p) != null) throw new IOException("Produto duplicado no arquivo.");
                }
                int movimentos = contagem(dados.readInt(), Modelo.LIMITE_MOVIMENTOS);
                List<Movimento> ms = new ArrayList<>();
                for (int i = 0; i < movimentos; i++) {
                    long id = dados.readLong(); Instant instante = Instant.ofEpochMilli(dados.readLong());
                    String codigo = dados.readUTF(), nome = dados.readUTF();
                    int tipo = dados.readUnsignedByte();
                    if (tipo > 1) throw new IOException("Tipo de movimento inválido.");
                    ms.add(new Movimento(id, instante, codigo, nome, Tipo.values()[tipo], dados.readInt(), dados.readUTF()));
                }
                if (dados.read() != -1) throw new IOException("Conteúdo adicional inesperado.");
                Dados resultado = new Dados(ps, ms); Modelo.validar(resultado); return resultado;
            }
        } catch (IllegalArgumentException | ArithmeticException e) {
            throw new IOException("Dados inválidos no arquivo: " + e.getMessage(), e);
        }
    }
    private static int contagem(int n, int max) throws IOException {
        if (n < 0 || n > max) throw new IOException("Contagem de registros inválida.");
        return n;
    }
    @Override public void close() throws IOException {
        if (!fechado) { fechado = true; try { trava.release(); } finally { canal.close(); } }
    }
}
