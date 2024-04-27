import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class Conta {
    private double saldo;

    public Conta(double saldoInicial) {
        this.saldo = saldoInicial;
    }

    public double getSaldo() {
        return saldo;
    }

    public synchronized void debitar(double valor) {
        saldo -= valor;
    }

    public synchronized void creditar(double valor) {
        saldo += valor;
    }
}

class Cliente extends Thread {
    private final Conta conta;
    private final String nome;
    private final Loja[] lojas;
    private Random random = new Random();

    public Cliente(String nome, Conta conta, Loja[] lojas) {
        this.nome = nome;
        this.conta = conta;
        this.lojas = lojas;
    }

    @Override
    public void run() {
        while (conta.getSaldo() > 0) {
            double valorCompra = random.nextInt(2) == 0 ? 100 : 200; // 100 ou 200
            int lojaIndex = random.nextInt(lojas.length);
            Loja loja = lojas[lojaIndex];
            synchronized (conta) {
                if (conta.getSaldo() >= valorCompra) {
                    conta.debitar(valorCompra);
                    loja.receberPagamento(valorCompra);
                    System.out.println(nome + " realizou compra de " + valorCompra + " na loja " + (lojaIndex + 1));
                } else {
                    System.out.println("Saldo insuficiente para realizar compra de " + valorCompra);
                    break; // Sai do loop se o saldo for insuficiente
                }
            }
            try {
                Thread.sleep(1000); // Espera um segundo entre as compras
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

class Loja {
    private final Conta conta;
    private final double salarioFuncionario = 1400.00;

    public Loja(Conta conta) {
        this.conta = conta;
    }

    public synchronized void receberPagamento(double valor) {
        conta.creditar(valor);
    }

    public synchronized void pagarFuncionarios() {
        double totalSalarios = salarioFuncionario * 2; // Dois funcionários por loja
        if (conta.getSaldo() >= totalSalarios) {
            conta.debitar(totalSalarios);
            System.out.println("Funcionários pagos na loja. Saldo restante: " + conta.getSaldo());
        } else {
            System.out.println("Saldo insuficiente para pagar funcionários na loja.");
        }
    }
}

class Funcionario extends Thread {
    private final Conta contaSalario;
    private final Conta contaInvestimentos;

    public Funcionario(Conta contaSalario, Conta contaInvestimentos) {
        this.contaSalario = contaSalario;
        this.contaInvestimentos = contaInvestimentos;
    }

    @Override
    public void run() {
        while (true) {
            synchronized (contaSalario) {
                double salario = 1400;
                contaSalario.creditar(salario);
                double valorInvestido = salario * 0.2;
                contaSalario.debitar(valorInvestido);
                contaInvestimentos.creditar(valorInvestido);
                System.out.println("Funcionário recebeu salário e investiu " + valorInvestido);
            }
            try {
                Thread.sleep(2000); // Espera dois segundos antes do próximo ciclo de pagamento
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

class Banco {
    private final Lock lock = new ReentrantLock();

    public void transferir(Conta origem, Conta destino, double valor) {
        lock.lock();
        try {
            origem.debitar(valor);
            destino.creditar(valor);
            System.out.println("Transferência de " + valor + " realizada.");
        } finally {
            lock.unlock();
        }
    }
}

public class SistemaBancario {
    public static void main(String[] args) {
        Conta contaBanco = new Conta(0); // Saldo inicial do banco
        Conta contaLoja1 = new Conta(0); // Saldo inicial da loja 1
        Conta contaLoja2 = new Conta(0); // Saldo inicial da loja 2

        Loja loja1 = new Loja(contaLoja1);
        Loja loja2 = new Loja(contaLoja2);

        Funcionario[] funcionariosLoja1 = { new Funcionario(contaLoja1, contaBanco), new Funcionario(contaLoja1, contaBanco) };
        Funcionario[] funcionariosLoja2 = { new Funcionario(contaLoja2, contaBanco), new Funcionario(contaLoja2, contaBanco) };

        Cliente[] clientes = {
            new Cliente("Cliente 1", contaBanco, new Loja[]{loja1, loja2}),
            new Cliente("Cliente 2", contaBanco, new Loja[]{loja1, loja2}),
            new Cliente("Cliente 3", contaBanco, new Loja[]{loja1, loja2}),
            new Cliente("Cliente 4", contaBanco, new Loja[]{loja1, loja2}),
            new Cliente("Cliente 5", contaBanco, new Loja[]{loja1, loja2})
        };

        // Inicializa os funcionários da loja 1
        for (Funcionario funcionario : funcionariosLoja1) {
            funcionario.start();
        }

        // Inicializa os funcionários da loja 2
        for (Funcionario funcionario : funcionariosLoja2) {
            funcionario.start();
        }

        // Inicializa os clientes
        for (Cliente cliente : clientes) {
            cliente.start();
        }

        // Aguarda a conclusão das threads dos clientes
        try {
            for (Cliente cliente : clientes) {
                cliente.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        loja1.pagarFuncionarios();
        loja2.pagarFuncionarios();
        System.out.println("Saldo final do banco: " + contaBanco.getSaldo());
        System.out.println("Saldo final da loja 1: " + contaLoja1.getSaldo());
        System.out.println("Saldo final da loja 2: " + contaLoja2.getSaldo());
    }
}
