
# Apache FOP

# Como executar o FOP

* O script principal se encontra em 

        fop-2.3/fop/fop  (sim, ele se chama fop)
    * Você pode testá-lo digitando `./fop` , desde que já tenha inserido as permissões. 

* Existe um outro script, tambem chamado fop, que tem apenas uma função: chamar o script fop citado acima. Ele foi criado para que executemos o fop dentro de qualquer outram pasta

* O script fopProfile funciona da mesma maneira, com a diferença de que foi feito para Profiling (mais informações em tópicos futuros)

* O arquivo fop.jar é apenas um demonstrativo e não é o executável que rodamosm após modificar o programa. Mesmo assim, aconselhamos que não o apaguem. 

## Como compilar o FOP

* Precisamos usar ou o maven ou o ant
* Ou você pode simplesmente rodar o script `compile.sh`

## Como editar o FOP

* Use seu editor de texto preferido
	* No VSCode, eu sugiro que clique em File > Open Folder e selecione a pasta fop-core. 
	* Um pom-xml faz a IDE entender que se trata de um projeto
* Quase tudo o que você precisa está na pasta: 
        
        fop-2.3/fop-core/src/main/java/org/apache/fop

* A função main se encontra em 
        
        fop-2.3/fop-core/src/main/java/org/apache/fop/cli



# Profile

## O que é profiling?

Fazer o profile de um código significa executá-lo e registrar todos e quaisquer dados relativos à performance, tais como: 

* Fluxo de execução
    * Aqui conseguimos ver quais classes chamaram quais métodos
* Tempo de execução de cada método
* Quantidade de vezes que cada método é chamado

Com isso, temos noção dos gargalos do código, isso é, trechos *pouco otimizados* do programa que atrasam a execução de vários outros trechos. 

## Como executar o profile do FOP: 
 
* JavaVisualVM instalado
* Instale o plugin Startup Profiler no JavaVisualVM

Segue o link: https://visualvm.github.io/startupprofiler.html

* Escolha a opção da CPU
* Na seção "Start profiling from classes:", digite:

        org.apache.fop.cli.Main
* Desmarque a opção "Profile new runnables"
* Marque o círculo "Profile only classes", e na seção escreva: 

        org.apache.*,

* Copie o conteúdo da clipboard que aparece após clicar em "Continue>>"
    * Adicione essa tag no final do script que chama o fop
    * NOTA: já fiz um script que faz isso por default, se chama `fopProfile`, na mesma pasta

* Tudo está configurado: clique em ` Profile `
* Rode o `fopProfile` com os parâmetros que quiser para o fop
* Aguarde o profile terminar
