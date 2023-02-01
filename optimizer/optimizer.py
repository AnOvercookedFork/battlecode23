import os
import random
import time
import signal
import subprocess

# constants
maps = ['maptestsmall', 'SmallElements', 'AllElements', 'DefaultMap']
constants_to_optimize = ['weight1', 'weight2']
resolution = [0.1, 0.1] # smallest significant change
bot_to_optimize = 'sphere'
optimized_name = 'optimized_sphere'
additional_args = '-Pdebug=false -Penableprofiler=false -PoutputVerbose=false -PshowIndicators=false'
max_subprocesses = 5

# optimizer constants
num_individuals = 5
max_rounds = 100

crossover_points = 2
crossover_chance = 0.8

# global vars because i am lazy
current_round = 0
current_best = []

bot_template = ('constant 1', 'constant 2') # bots are represented as tuples of constants in the same order as the constants that are provided to optimize
# run format:
# gradlew run -Pmaps=[map] -PteamA=[Team A] -PteamB=[Team B]

# mutate one stat by random * resolution
def mutate(bot):
    mutated = bot.copy()
    index = random.randint(0, len(mutated) - 1)
    magnitude = random.randint(-2, 2)

    mutated[index] += magnitude * resolution[index]
    return mutated


def crossover(parent1, parent2):
   if random.random() > crossover_chance:
      return parent1
   
   new_bot = parent1.copy()
   indices = random.sample(range(0, len(constants_to_optimize)), crossover_points)
   
   for index in indices:               #takes letters from selected (random) indices and puts them in the same spot in the child
      new_bot[index] = parent2[index]
   
   return new_bot

def get_winner(data):
    # does weird stuff if client is open, opened, or closed
    result = data[-5].split(' ')[-4][1:2]
    if result == 'A' or result == 'B':
        return result
    result = data[-6].split(' ')[-4][1:2]
    if result == 'A' or result == 'B':
        return result
    return data[-7].split(' ')[-4][1:2]


def run_matches(teamA, teamB, score):
    for map in maps:
        current = time.time()
        result = os.popen('gradlew run -Pmaps={map} -PteamA={a} -PteamB={b} -Pdebug=false -Penableprofiler=false -PoutputVerbose=false -PshowIndicators=false'.format(map = map, a = teamA, b = teamB)).read().splitlines()
        print('Last round {} took {} seconds'.format(map, time.time() - current))
        winner = get_winner(result)
        if winner == 'A':
            score[teamA] += 1
        elif winner == 'B':
            score[teamB] += 1
        else:
            print('I don\'t know who won this, take a look: {}'.format(result))
    return score

def sigint_handler():
    print
    exit()

# may not need to build between each evolution, check on this?
def main():
    os.chdir('..')
    os.system('gradlew update')
    os.system('gradlew build')
    
    signal.signal(signal.SIGINT, sigint_handler)

    while(len(set_individuals.keys()) < num_individuals):  #make num_individuals unique keys
      try_key = random_key()
      set_individuals[try_key] = evaluate_fitness(try_key) #intitialize keys
      
      if set_individuals[try_key] > best_fitness:  #update best fitness
         best_fitness = set_individuals[try_key]
         best_key = try_key

if __name__ == '__main__':
    main()