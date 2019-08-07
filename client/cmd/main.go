package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"git.proxeus.com/ui/doctmpl"
	"github.com/prometheus/common/log"
	"io"
	"io/ioutil"
	"os"
	"path/filepath"
	"strings"
)

type arrayFlags []string

func (i *arrayFlags) String() string {
	return "asset files list"
}

func (i *arrayFlags) Set(value string) error {
	*i = append(*i, value)
	return nil
}

var assetFiles arrayFlags

func main() {
	var url = flag.String("u", "http://localhost:2115", "Document-Service URL")
	method := flag.String("m", "compile", "compile or vars")
	tmplPath := flag.String("t", "", "ODT template path")
	format := flag.String("f", "pdf", "result format, possible values: pdf, odt, docx or doc")
	dataPath := flag.String("d", "", "JSON file path")
	embedError := flag.Bool("e", false, "embed compilation error into the returned document")
	flag.Var(&assetFiles, "a", "asset files, provide directory or file like -a file1 -a file2 -a dir1")
	varPrefix := flag.String("p", "", "var prefix to filter vars")
	outPath := flag.String("o", "result", "output path, extension will be attached if not provided")
	flag.Usage = func() {
		fmt.Fprintf(os.Stderr, "Usage of %s:\ncompile usage:\n%s -t tmpl.odt -o my.pdf\n%s -u http://123.12.12.111:8888 -f pdf -t my/tmpl.odt -d data.json -a myImage.png -a my/images/dir -o output.pdf \nprint vars usage:\n%s -m vars -t my/tmpl.odt -p input.\n\n", os.Args[0], os.Args[0], os.Args[0], os.Args[0])
		flag.PrintDefaults()
	}
	flag.Parse()
	if *tmplPath == "" {
		fmt.Println("template path must be provided, please use -t")
		fmt.Println()
		flag.Usage()
		os.Exit(1)
	}
	ds := &doctmpl.DocumentServiceClient{Url: *url}
	if *method == "compile" {
		form := doctmpl.Format(*format)
		r, err := ds.Compile(doctmpl.Template{
			Format:       form,
			Data:         provideData(*dataPath),
			TemplatePath: *tmplPath,
			Assets:       getFilesListOnly(assetFiles),
			EmbedError:   *embedError,
		})
		if err != nil {
			log.Error(err)
			os.Exit(1)
		}
		out := *outPath
		if !strings.Contains(filepath.Base(out), ".") {
			out = "." + form.String()
		}
		outFile, err := os.OpenFile(out, os.O_WRONLY|os.O_CREATE|os.O_TRUNC, 0600)
		if err != nil {
			log.Error(err)
			os.Exit(1)
		}
		defer r.Body.Close()
		defer outFile.Close()
		_, err = io.Copy(outFile, r.Body)
		if err != nil {
			log.Error(err)
			os.Exit(1)
		}
	} else if *method == "vars" {
		v, err := ds.Vars(*tmplPath, *varPrefix)
		if err != nil {
			log.Error(err)
			os.Exit(1)
		}
		fmt.Println(v)
	}
}

func provideData(jf string) interface{} {
	d := map[string]interface{}{}
	f, err := os.Open(jf)
	if err != nil {
		return d
	}
	defer f.Close()
	bts, err := ioutil.ReadAll(f)
	if err != nil {
		return d
	}
	err = json.Unmarshal(bts, d)
	if err != nil {
		return d
	}
	return d
}

//serializes directories to file entries
func getFilesListOnly(files []string) []string {
	serializedFiles := make([]string, 0)
	for _, a := range files {
		rec(&serializedFiles, a)
	}
	fmt.Println(serializedFiles)
	return serializedFiles
}

func rec(i *[]string, name string) {
	fi, err := os.Stat(name)
	if err != nil {
		fmt.Println(err)
		panic(err)
		return
	}
	switch mode := fi.Mode(); {
	case mode.IsDir():
		files, err := ioutil.ReadDir(name)
		if err != nil {
			fmt.Println(err)
			panic(err)
		}
		for _, f := range files {
			rec(i, filepath.Join(name, f.Name()))
		}
	case mode.IsRegular():
		*i = append(*i, name)
	}
}
