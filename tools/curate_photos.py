"""Curated photographic concepts for lesson cards (Wikimedia Commons photos)."""
import html
import json
import re
import urllib.parse
import urllib.request
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
USER_AGENT='HelloQuest educational Android prototype (photo research)'
# key | Commons search terms | lesson words separated by commas
PLAN='''
 mother|mother child|mother
 father|father child|father
 sister|two sisters|sister
 brother|two brothers|brother
 pilot|airplane pilot|a pilot,pilot
 student|student school|a student
 friend|friends together|my friend
 run|person running|run,she is running,it runs
 walk|person walking|walk,walked,he is walking,we walk
 jump|person jumping|jump,hop
 read|reading book|read,he reads,she reads,I am reading,she is going to read
 swim|person swimming|swim
 draw|person drawing|draw
 write|person writing|write
 sleep|person sleeping|sleep
 smile|person smiling|smile
 climb|person climbing|climb
 fly|airplane flying|fly
 sail|sailboat sailing|sail
 build|building house|build
 carry|carrying box|carry
 pack|packing suitcase|pack
 play|playing video games|play,I play,we play,she plays,he plays,we are playing,I am going to play
 help|people helping|help
 hide|person hiding|hide
 listen|person listening|listen,Listen
 open|opening door|open
 close|closing door|close
 repair|person repairing|repair
 explore|exploring outdoors|explore,travel,visit
 wait|person waiting|wait
 win|winning race|win
 ask|student asking question|ask
 think|person thinking|think
 look|person looking|look,is looking
 choose|person choosing|choose
 cat|cat portrait|cat,a cat
 dog|dog portrait|dog,a dog
 rabbit|rabbit animal|rabbit,a rabbit
 bird|bird animal|bird,a bird
 fish|fish underwater|fish,a fish
 bear|bear animal|a bear
 duck|duck animal|a duck
 fox|fox animal|a fox
 turtle|turtle animal|a turtle
 owl|owl bird|an owl
 butterfly|butterfly insect|a butterfly
 apple|red apple fruit|apple
 banana|banana fruit|banana
 bread|bread loaf|bread
 milk|glass of milk|milk
 water|glass of water|water
 beach|beach sea|a beach
 castle|castle building|a castle
 bridge|bridge river|a bridge
 cave|cave interior|a cave
 city|city skyline|a city
 moon|full moon night|moon,a moon
 planet|planet space|a planet
 star|stars sky|a star,yellow star
 ship|spaceship model|a ship
 robot|robot|a droid
 map|paper map|a map
 helmet|helmet|a helmet
 key|key closeup|a key
 book|book open|book,a book,books
 letter|paper letter envelope|a letter
 passport|passport travel|a passport
 suitcase|travel suitcase|a suitcase
 ticket|train ticket|a ticket,ticket
 train|train railway|a train
 station|train station|a station,station
 coin|coin currency|a coin
 candle|lit candle|a candle
 scarf|knitted scarf|scarf,a scarf
 hat|hat clothing|hat
 shoes|shoes sneakers|shoes
 coat|coat clothing|coat
 shirt|shirt clothing|shirt
 head|human face portrait|head
 hand|hand closeup|hand
 foot|bare foot|foot,feet
 eyes|eyes closeup|eyes
 ears|ear closeup|ears
 desk|desk school|desk
 notebook|notebook paper|notebook
 pen|pen writing|pen
 pencil|pencil|pencil
 door|wooden door|door,a door
 window|window house|window
 table|wooden table|table
 chair|chair furniture|chair
 park|public park|park
 hospital|hospital building|hospital
 library|library books|library
 museum|museum building|museum
 rainy|rain weather|rainy
 sunny|sunny sky|sunny
 winter|snow winter|winter
 summer|summer beach|summer
 cold|snow frost|cold
 happy|happy person|happy
 sad|sad person|sad
 scared|scared person|scared
 excited|excited child|excited
 calm|calm person|calm
 fast|fast car|fast
 small|small animal|small
 big|large building|big
 night|night sky|night
 green_light|green traffic light|green light
 wings|bird wings|wings
'''


def terms():
    rows={}
    for line in PLAN.strip().splitlines():
        key,query,aliases=[s.strip() for s in line.split('|')]
        rows[key]=(query,[x.strip() for x in aliases.split(',')])
    return rows


def search(query):
    text=query+' incategory:"Images from Unsplash"'
    url='https://commons.wikimedia.org/w/index.php?'+urllib.parse.urlencode({
        'title':'Special:Search','search':text,'ns6':'1'})
    request=urllib.request.Request(url,headers={'User-Agent':USER_AGENT})
    data=urllib.request.urlopen(request,timeout=20).read().decode()
    return [html.unescape(re.sub('<[^>]+>','',m.group())).strip()
            for m in re.finditer(r'<div class="mw-search-result-heading">.*?</div>',data,re.S)]


if __name__=='__main__':
    import sys,time
    if '--candidates' in sys.argv:
        output={}
        for index,(key,(query,aliases)) in enumerate(terms().items()):
            try: output[key]=search(query)[:10]
            except Exception as error:output[key]=['ERROR '+str(error)]
            print(index+1,key,'=>',' || '.join(output[key][:3]),flush=True)
            time.sleep(.4)
        (ROOT/'content/photo-candidates.json').write_text(json.dumps(output,ensure_ascii=False,indent=2)+'\n')
